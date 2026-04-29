package com.example.questionbank.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import lombok.Data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 识别后的题目草稿，供前端确认/编辑后再提交
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuestionDraft {
    /** 题型：single / multiple / judge / fill / essay */
    private String type;

    /** 题目内容（题干） */
    private String content;

    /**
     * 选项列表，兼容两种模型输出格式：
     *   - 对象数组：[{"label":"A","text":"选项内容"}, ...]
     *   - 字符串数组：["A. 选项内容", "B. 选项内容", ...]
     */
    @JsonDeserialize(using = OptionsDeserializer.class)
    private List<OptionItem> options;

    /** 正确答案 */
    private Object answer;

    /** 答案解析（可为 null） */
    private String analysis;

    /** 难度：easy / medium / hard */
    private String difficulty;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OptionItem {
        private String label;
        private String text;

        public OptionItem() {}

        public OptionItem(String label, String text) {
            this.label = label;
            this.text = text;
        }
    }

    /**
     * 自定义反序列化器：兼容 options 为对象数组或字符串数组两种格式
     */
    public static class OptionsDeserializer extends StdDeserializer<List<OptionItem>> {

        public OptionsDeserializer() {
            super(List.class);
        }

        @Override
        public List<OptionItem> deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            if (node.isNull()) return null;

            List<OptionItem> result = new ArrayList<>();
            if (!node.isArray()) return result;

            for (JsonNode item : node) {
                if (item.isTextual()) {
                    // 字符串格式："A. 选项内容" 或 "A、选项内容" 或 "A选项内容"
                    String raw = item.asText().trim();
                    result.add(parseStringOption(raw));
                } else if (item.isObject()) {
                    // 对象格式：{"label":"A","text":"..."}
                    String label = item.path("label").asText("");
                    // 兼容 key 为 "option"、"value"、"content" 等变体
                    String text = item.path("text").asText(
                            item.path("content").asText(
                            item.path("value").asText(
                            item.path("option").asText(""))));
                    result.add(new OptionItem(label, text));
                }
            }
            return result;
        }

        /** 将 "A. 选项内容" / "A、选项内容" / "A) 选项内容" 拆分为 label + text */
        private OptionItem parseStringOption(String raw) {
            // 匹配开头的单个大写字母后跟 . / 、/ ) / ：/ : / 空格
            if (raw.length() >= 2) {
                char first = raw.charAt(0);
                if (first >= 'A' && first <= 'Z') {
                    char sep = raw.charAt(1);
                    if (sep == '.' || sep == '、' || sep == ')' || sep == '：'
                            || sep == ':' || sep == ' ' || sep == '）') {
                        String label = String.valueOf(first);
                        String text = raw.substring(2).trim();
                        return new OptionItem(label, text);
                    }
                }
                // 小写字母同样处理
                if (first >= 'a' && first <= 'z') {
                    char sep = raw.charAt(1);
                    if (sep == '.' || sep == ')' || sep == ' ') {
                        String label = String.valueOf(first).toUpperCase();
                        String text = raw.substring(2).trim();
                        return new OptionItem(label, text);
                    }
                }
            }
            // 无法解析前缀，整体作为 text，label 留空
            return new OptionItem("", raw);
        }
    }
}
