package com.example.questionbank.service.impl;

import com.example.questionbank.dto.response.QuestionDraft;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Slf4j
@Service
public class DashScopeService {

    @Value("${dashscope.api-key}")
    private String apiKey;

    /** 纯文本/文件识别模型（qwen3.5-plus，支持关闭深度思考） */
    @Value("${dashscope.text-model}")
    private String textModel;

    /** 图片识别模型（多模态） */
    @Value("${dashscope.vision-model}")
    private String visionModel;

    @Value("${dashscope.base-url}")
    private String baseUrl;

    // AI 识别可能耗时较长，设置 120s 读取超时
    private final RestTemplate restTemplate = buildRestTemplate();

    private static RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);   // 连接超时 10s
        factory.setReadTimeout(120_000);     // 读取超时 120s
        return new RestTemplate(factory);
    }
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * System prompt：简洁明确，配合 json_schema 约束输出，无需冗长描述
     */
    private static final String SYSTEM_PROMPT =
            "你是题目提取助手。从用户提供的内容中提取所有题目，严格按指定 JSON 格式输出，每道题必须是包含 type/content/options/answer/difficulty 字段的完整对象，不得输出字符串或其他格式，不输出任何解释性文字。";

    /**
     * 用于约束输出结构的 JSON Schema（questions 数组）
     * 官方文档：response_format.type = json_schema 可强制模型输出符合 schema 的 JSON
     */
    private static final Map<String, Object> JSON_SCHEMA = buildSchema();

    private static Map<String, Object> buildSchema() {
        // option item schema
        Map<String, Object> optionProps = new LinkedHashMap<>();
        optionProps.put("label", Map.of("type", "string", "description", "选项字母，如 A、B、C、D"));
        optionProps.put("text", Map.of("type", "string", "description", "选项内容"));

        Map<String, Object> optionSchema = new LinkedHashMap<>();
        optionSchema.put("type", "object");
        optionSchema.put("properties", optionProps);
        optionSchema.put("required", List.of("label", "text"));
        optionSchema.put("additionalProperties", false);

        // question item schema
        Map<String, Object> questionProps = new LinkedHashMap<>();
        questionProps.put("type", Map.of(
                "type", "string",
                "enum", List.of("single", "multiple", "judge", "fill", "essay"),
                "description", "题型：single单选/multiple多选/judge判断/fill填空/essay简答"
        ));
        questionProps.put("content", Map.of("type", "string", "description", "题干文字"));
        questionProps.put("options", Map.of(
                "type", List.of("array", "null"),
                "items", optionSchema,
                "description", "选项列表，单选/多选题必填，其他题型为 null"
        ));
        questionProps.put("answer", Map.of(
                "description", "答案：单选为字母字符串如\"A\"，多选为字母数组如[\"A\",\"C\"]，判断为\"true\"或\"false\"，填空/简答为字符串"
        ));
        questionProps.put("analysis", Map.of(
                "type", List.of("string", "null"),
                "description", "答案解析，无则为 null"
        ));
        questionProps.put("difficulty", Map.of(
                "type", "string",
                "enum", List.of("easy", "medium", "hard"),
                "description", "难度：easy简单/medium中等/hard困难，根据题目内容判断"
        ));

        Map<String, Object> questionSchema = new LinkedHashMap<>();
        questionSchema.put("type", "object");
        questionSchema.put("properties", questionProps);
        questionSchema.put("required", List.of("type", "content", "options", "answer", "difficulty"));
        questionSchema.put("additionalProperties", false);

        // root schema: { questions: [...] }
        Map<String, Object> rootProps = new LinkedHashMap<>();
        rootProps.put("questions", Map.of(
                "type", "array",
                "items", questionSchema,
                "description", "提取到的所有题目"
        ));

        Map<String, Object> rootSchema = new LinkedHashMap<>();
        rootSchema.put("type", "object");
        rootSchema.put("properties", rootProps);
        rootSchema.put("required", List.of("questions"));
        rootSchema.put("additionalProperties", false);

        // response_format 包装
        Map<String, Object> jsonSchemaWrapper = new LinkedHashMap<>();
        jsonSchemaWrapper.put("name", "question_list");
        jsonSchemaWrapper.put("strict", true);
        jsonSchemaWrapper.put("schema", rootSchema);

        Map<String, Object> responseFormat = new LinkedHashMap<>();
        responseFormat.put("type", "json_schema");
        responseFormat.put("json_schema", jsonSchemaWrapper);

        return responseFormat;
    }

    /**
     * 解析纯文本中的题目（使用 qwen3.5-plus，关闭深度思考）
     */
    public List<QuestionDraft> parseFromText(String text) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(buildSystemMessage());
        messages.add(Map.of("role", "user", "content", text));
        return callApi(messages, textModel, false);
    }

    /**
     * 流式解析纯文本中的题目，每识别出一道完整题目立即回调 onQuestion。
     */
    public void parseFromTextStream(String text, Consumer<QuestionDraft> onQuestion) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(buildSystemMessage());
        messages.add(Map.of("role", "user", "content", text));
        callApiStream(messages, textModel, false, onQuestion);
    }

    /**
     * 流式解析纯文本中的题目（可取消）。
     */
    public void parseFromTextStream(String text, Consumer<QuestionDraft> onQuestion, AtomicBoolean cancelled) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(buildSystemMessage());
        messages.add(Map.of("role", "user", "content", text));
        callApiStream(messages, textModel, false, onQuestion, cancelled);
    }

    /**
     * 流式解析单张图片（可取消）。
     */
    public void parseFromImageStream(String imageBase64, Consumer<QuestionDraft> onQuestion, AtomicBoolean cancelled) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(buildSystemMessage());

        String b64 = imageBase64;
        String mimeType = (b64 != null && b64.startsWith("/9j/")) ? "image/jpeg" : "image/png";
        Map<String, Object> imgPart = new HashMap<>();
        imgPart.put("type", "image_url");
        imgPart.put("image_url", Map.of("url", "data:" + mimeType + ";base64," + b64));

        List<Map<String, Object>> contentParts = new ArrayList<>();
        contentParts.add(imgPart);
        contentParts.add(Map.of("type", "text", "text", "提取图片中所有题目。"));

        messages.add(Map.of("role", "user", "content", contentParts));
        callApiStream(messages, visionModel, true, onQuestion, cancelled);
    }

    /**
     * 解析图片列表中的题目（使用 qwen-vl-plus-latest 多模态）
     */
    public List<QuestionDraft> parseFromImages(List<String> imageBase64List) {
        // 多张图片一次性识别时，模型输出可能非常长，容易触发输出截断导致 JSON 不完整。
        // 这里改为逐张图片识别并合并结果，显著降低单次输出长度，提高稳定性。
        List<QuestionDraft> all = new ArrayList<>();
        if (imageBase64List == null || imageBase64List.isEmpty()) return all;

        for (int idx = 0; idx < imageBase64List.size(); idx++) {
            String b64 = imageBase64List.get(idx);
            try {
                all.addAll(parseFromSingleImage(b64));
            } catch (Exception e) {
                log.error("Parse image {} failed", idx + 1, e);
                throw new RuntimeException("第 " + (idx + 1) + " 张图片识别失败：" + e.getMessage());
            }
        }
        return all;
    }

    private List<QuestionDraft> parseFromSingleImage(String b64) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(buildSystemMessage());

        String mimeType = (b64 != null && b64.startsWith("/9j/")) ? "image/jpeg" : "image/png";
        Map<String, Object> imgPart = new HashMap<>();
        imgPart.put("type", "image_url");
        imgPart.put("image_url", Map.of("url", "data:" + mimeType + ";base64," + b64));

        List<Map<String, Object>> contentParts = new ArrayList<>();
        contentParts.add(imgPart);
        contentParts.add(Map.of("type", "text", "text", "提取图片中所有题目。"));

        messages.add(Map.of("role", "user", "content", contentParts));
        // 视觉模型不支持 enable_thinking
        return callApi(messages, visionModel, true);
    }

    private Map<String, Object> buildSystemMessage() {
        return Map.of("role", "system", "content", SYSTEM_PROMPT);
    }

    /**
     * @param messages      消息列表
     * @param model         模型名称
     * @param isVisionModel 是否为视觉模型（视觉模型不支持 enable_thinking 参数）
     */
    private List<QuestionDraft> callApi(List<Map<String, Object>> messages, String model, boolean isVisionModel) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);

        // 使用 json_schema 精确约束输出，彻底避免废话
        body.put("response_format", JSON_SCHEMA);

        // 低温度：输出更确定，减少随机性
        body.put("temperature", 0.1);

        // qwen3.5-plus 等混合思考模型：显式关闭深度思考，节省 token 和时间
        if (!isVisionModel) {
            body.put("enable_thinking", false);
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl + "/chat/completions", request, String.class);

            String responseBody = response.getBody();
            log.debug("DashScope raw response: {}", responseBody);

            JsonNode root = objectMapper.readTree(responseBody);

            // 检查 API 错误
            if (root.has("error")) {
                String errMsg = root.path("error").path("message").asText("未知错误");
                throw new RuntimeException("DashScope API 错误：" + errMsg);
            }

            JsonNode choice0 = root.path("choices").get(0);
            String finishReason = choice0.path("finish_reason").asText("");
            String content = choice0.path("message").path("content").asText();
            log.debug("DashScope content: {}", content);

            // json_schema 模式下模型直接输出合法 JSON，无需清理 markdown 标记
            JsonNode parsed;
            try {
                parsed = objectMapper.readTree(content);
            } catch (Exception e) {
                // 常见原因：输出被截断（finish_reason=length），导致 JSON 不完整
                if ("length".equalsIgnoreCase(finishReason)) {
                    throw new RuntimeException("AI 输出过长被截断，请减少单次识别内容（例如拆分 PDF/图片）后重试");
                }
                throw e;
            }

            // 提取 questions 数组（schema 约束输出为 {"questions": [...]}）
            JsonNode questionsNode = parsed.path("questions");
            if (questionsNode.isMissingNode() || !questionsNode.isArray()) {
                // 兼容模型直接返回数组的情况
                if (parsed.isArray()) {
                    questionsNode = parsed;
                } else {
                    // 找第一个数组字段
                    Iterator<Map.Entry<String, JsonNode>> fields = parsed.fields();
                    while (fields.hasNext()) {
                        JsonNode val = fields.next().getValue();
                        if (val.isArray()) { questionsNode = val; break; }
                    }
                }
            }

            // 处理模型将每道题包裹在单字段对象中的情况，如 [{"question":{...}}, ...]
            questionsNode = unwrapIfNeeded(questionsNode);

            // 处理数组元素为字符串的情况（模型未遵守 schema，直接输出题干文本）
            questionsNode = normalizeElements(questionsNode);

            return objectMapper.convertValue(questionsNode, new TypeReference<List<QuestionDraft>>() {});

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("DashScope API call failed", e);
            throw new RuntimeException("AI 识别失败：" + e.getMessage());
        }
    }

    /**
     * 处理模型将每道题目包裹在单字段对象中的情况。
     * 例如 [{"question": {...}}, ...] → [{...}, ...]
     * 判断依据：数组第一个元素是只有一个字段的对象，且该字段值也是对象。
     */
    private JsonNode unwrapIfNeeded(JsonNode arrayNode) {
        if (!arrayNode.isArray() || arrayNode.isEmpty()) return arrayNode;
        JsonNode first = arrayNode.get(0);
        if (!first.isObject() || first.size() != 1) return arrayNode;

        String singleKey = first.fieldNames().next();
        JsonNode inner = first.get(singleKey);
        if (!inner.isObject()) return arrayNode;

        // 确认内层对象包含题目字段（至少有 type 或 content）
        if (!inner.has("type") && !inner.has("content")) return arrayNode;

        // 展开：将每个元素的单字段值提取出来
        com.fasterxml.jackson.databind.node.ArrayNode unwrapped =
                objectMapper.createArrayNode();
        for (JsonNode item : arrayNode) {
            if (item.isObject() && item.size() == 1) {
                JsonNode val = item.fields().next().getValue();
                unwrapped.add(val.isObject() ? val : item);
            } else {
                unwrapped.add(item);
            }
        }
        log.debug("Unwrapped question array from key '{}'", singleKey);
        return unwrapped;
    }

    /**
     * 流式调用 DashScope API，逐 token 拼接，检测到完整 JSON 对象时立即回调。
     * 模型输出格式：{"questions":[{...},{...}]}
     * 通过括号深度计数器检测每个 question 对象的边界。
     */
    private void callApiStream(List<Map<String, Object>> messages, String model,
                               boolean isVisionModel, Consumer<QuestionDraft> onQuestion) {
        callApiStream(messages, model, isVisionModel, onQuestion, new AtomicBoolean(false));
    }

    private void callApiStream(List<Map<String, Object>> messages, String model,
                               boolean isVisionModel, Consumer<QuestionDraft> onQuestion, AtomicBoolean cancelled) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("stream", true);
        // 流式模式也使用 json_schema 约束输出结构，便于按题目对象边界解析
        body.put("response_format", JSON_SCHEMA);
        body.put("temperature", 0.1);
        if (!isVisionModel) {
            body.put("enable_thinking", false);
        }

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        restTemplate.execute(
            baseUrl + "/chat/completions",
            HttpMethod.POST,
            req -> {
                req.getHeaders().putAll(headers);
                objectMapper.writeValue(req.getBody(), body);
            },
            response -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.getBody()))) {

                    StringBuilder tokenBuffer = new StringBuilder();
                    StringBuilder preamble = new StringBuilder();
                    boolean insideArray = false;
                    boolean foundQuestionsKey = false;
                    boolean inString = false;
                    boolean escape = false;
                    int depth = 0;
                    StringBuilder objBuffer = new StringBuilder();
                    boolean truncated = false;

                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (cancelled != null && cancelled.get()) break;
                        if (!line.startsWith("data:")) continue;
                        String data = line.substring(5).trim();
                        if ("[DONE]".equals(data)) break;

                        try {
                            JsonNode chunk = objectMapper.readTree(data);
                            JsonNode choice0 = chunk.path("choices").get(0);
                            String fr = choice0.path("finish_reason").asText("");
                            if ("length".equalsIgnoreCase(fr)) truncated = true;
                            String delta = choice0.path("delta").path("content").asText("");
                            if (delta.isEmpty()) continue;
                            tokenBuffer.append(delta);
                        } catch (Exception ignored) {
                            continue;
                        }

                        // 从 tokenBuffer 中逐字符扫描，提取完整的 question 对象
                        String buf = tokenBuffer.toString();
                        tokenBuffer.setLength(0);

                        for (int i = 0; i < buf.length(); i++) {
                            if (cancelled != null && cancelled.get()) break;
                            char c = buf.charAt(i);

                            // 字符串状态机：避免把字符串里的 { } 当成结构
                            if (inString) {
                                if (escape) {
                                    escape = false;
                                } else if (c == '\\') {
                                    escape = true;
                                } else if (c == '\"') {
                                    inString = false;
                                }
                            } else {
                                if (c == '\"') inString = true;
                            }

                            if (!insideArray) {
                                if (preamble.length() < 2000) preamble.append(c);
                                if (!foundQuestionsKey && preamble.indexOf("\"questions\"") >= 0) {
                                    foundQuestionsKey = true;
                                }
                                if (foundQuestionsKey && c == '[') {
                                    insideArray = true;
                                }
                                continue;
                            }

                            if (depth == 0) {
                                if (c != '{') continue;
                            }

                            objBuffer.append(c);
                            if (!inString) {
                                if (c == '{') {
                                    depth++;
                                } else if (c == '}') {
                                    depth--;
                                    if (depth == 0) {
                                        String objJson = objBuffer.toString().trim();
                                        objBuffer.setLength(0);
                                        try {
                                            QuestionDraft draft = objectMapper.readValue(objJson, QuestionDraft.class);
                                            if (draft != null && draft.getContent() != null && !draft.getContent().isBlank()) {
                                                onQuestion.accept(draft);
                                            }
                                        } catch (Exception e) {
                                            log.warn("Failed to parse streamed question object: {}", objJson, e);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 如果输出被截断，最后可能还有未闭合对象
                    if (!cancelled.get() && truncated && (depth != 0 || objBuffer.length() > 0)) {
                        throw new RuntimeException("AI 输出过长被截断，请减少单次识别内容（例如拆分 PDF/图片）后重试");
                    }
                }
                return null;
            }
        );
    }

    /**
     * 将数组中的字符串元素转换为最小可用的 QuestionDraft 对象节点。
     * 模型有时不遵守 json_schema，直接把题干文本放进数组。
     * 例如 ["题干A", "题干B"] → [{"type":"essay","content":"题干A",...}, ...]
     */
    private JsonNode normalizeElements(JsonNode arrayNode) {
        if (!arrayNode.isArray() || arrayNode.isEmpty()) return arrayNode;

        // 检查是否存在字符串元素
        boolean hasStringElement = false;
        for (JsonNode item : arrayNode) {
            if (item.isTextual()) { hasStringElement = true; break; }
        }
        if (!hasStringElement) return arrayNode;

        com.fasterxml.jackson.databind.node.ArrayNode normalized = objectMapper.createArrayNode();
        for (JsonNode item : arrayNode) {
            if (item.isTextual()) {
                com.fasterxml.jackson.databind.node.ObjectNode obj = objectMapper.createObjectNode();
                obj.put("type", "essay");
                obj.put("content", item.asText());
                obj.putNull("options");
                obj.put("answer", "");
                obj.putNull("analysis");
                obj.put("difficulty", "medium");
                normalized.add(obj);
                log.debug("Converted string element to QuestionDraft: {}", item.asText());
            } else {
                normalized.add(item);
            }
        }
        return normalized;
    }
}
