package com.example.questionbank.controller;

import com.example.questionbank.common.Result;
import com.example.questionbank.dto.request.AiParseRequest;
import com.example.questionbank.dto.response.QuestionDraft;
import com.example.questionbank.service.impl.DashScopeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Tag(name = "AI 题目识别")
@RestController
@RequestMapping("/ai")
public class AiParseController {

    @Autowired
    private DashScopeService dashScopeService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final long STREAM_SESSION_TTL_MS = 5 * 60 * 1000L; // 5分钟
    private static final Map<String, PendingStream> STREAM_SESSIONS = new ConcurrentHashMap<>();

    private enum StreamMode { TEXT_CHUNKS, IMAGES }

    private static class TextChunk {
        final String text;
        final int index;
        final int total;
        final Integer pageFrom;
        final Integer pageTo;

        TextChunk(String text, int index, int total, Integer pageFrom, Integer pageTo) {
            this.text = text;
            this.index = index;
            this.total = total;
            this.pageFrom = pageFrom;
            this.pageTo = pageTo;
        }
    }

    private static class PendingStream {
        final String owner;
        final StreamMode mode;
        final List<TextChunk> chunks;
        final List<String> images;
        final long createdAt;

        PendingStream(String owner, StreamMode mode, List<TextChunk> chunks, List<String> images, long createdAt) {
            this.owner = owner;
            this.mode = mode;
            this.chunks = chunks;
            this.images = images;
            this.createdAt = createdAt;
        }
    }

    private void cleanupExpiredSessions() {
        long now = System.currentTimeMillis();
        STREAM_SESSIONS.entrySet().removeIf(e -> now - e.getValue().createdAt > STREAM_SESSION_TTL_MS);
    }

    @Operation(summary = "AI 批量识别题目", description = "支持文本、文件（TXT/DOCX/PDF）、图片三种输入方式")
    @PostMapping("/parse")
    @PreAuthorize("isAuthenticated()")
    public Result<List<QuestionDraft>> parse(@RequestBody AiParseRequest request) {
        try {
            // 优先级：图片 > 文件 > 文本
            if (request.getImageBase64List() != null && !request.getImageBase64List().isEmpty()) {
                return Result.success(dashScopeService.parseFromImages(request.getImageBase64List()));
            }

            if (request.getFileBase64() != null && !request.getFileBase64().isBlank()) {
                String text = extractTextFromFile(request.getFileBase64(), request.getFileType());
                return Result.success(dashScopeService.parseFromText(text));
            }

            if (request.getText() != null && !request.getText().isBlank()) {
                return Result.success(dashScopeService.parseFromText(request.getText()));
            }

            return Result.error("请提供题目内容（文本、文件或图片）");

        } catch (Exception e) {
            log.error("AI parse failed", e);
            return Result.error("识别失败：" + e.getMessage());
        }
    }

    @Operation(summary = "AI 流式识别初始化（返回 sessionKey）", description = "文本/文件/图片统一入口。先 POST 初始化，再用 sessionKey 建立 SSE，识别多少推多少。")
    @PostMapping("/parse/stream/init")
    @PreAuthorize("isAuthenticated()")
    public Result<Map<String, String>> parseStreamInit(@RequestBody AiParseRequest request) {
        try {
            cleanupExpiredSessions();

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String owner = (auth != null) ? auth.getName() : "anonymous";

            String sessionKey = UUID.randomUUID().toString();
            PendingStream pending = buildPendingStream(owner, request);
            STREAM_SESSIONS.put(sessionKey, pending);

            return Result.success(Map.of("sessionKey", sessionKey));
        } catch (Exception e) {
            log.error("SSE init failed", e);
            return Result.error("初始化失败：" + e.getMessage());
        }
    }

    @Operation(summary = "AI 流式识别题目（SSE）", description = "文本/文件输入，识别出一道题立即推送，不等待全部完成")
    @GetMapping(value = "/parse/stream", produces = "text/event-stream;charset=UTF-8")
    @PreAuthorize("isAuthenticated()")
    public SseEmitter parseStream(
            @RequestParam String sessionKey) {

        SseEmitter emitter = new SseEmitter(120_000L);
        // 注意：SseEmitter 使用异步线程发送数据，SecurityContext 不会自动传播到新线程
        // 必须在进入异步前取出当前用户名并传入异步任务，否则会变成 anonymous 导致鉴权失败
        Authentication authOnRequest = SecurityContextHolder.getContext().getAuthentication();
        final String currentUser = (authOnRequest != null) ? authOnRequest.getName() : "anonymous";
        final AtomicBoolean cancelled = new AtomicBoolean(false);
        emitter.onCompletion(() -> cancelled.set(true));
        emitter.onTimeout(() -> cancelled.set(true));
        emitter.onError((e) -> cancelled.set(true));

        CompletableFuture.runAsync(() -> {
            try {
                cleanupExpiredSessions();

                PendingStream pending = STREAM_SESSIONS.remove(sessionKey);
                if (pending == null) {
                    emitter.send(SseEmitter.event().name("server_error").data("识别会话已过期，请重新发起识别"));
                    emitter.complete();
                    return;
                }

                if (pending.owner != null && !pending.owner.equals(currentUser)) {
                    emitter.send(SseEmitter.event().name("server_error").data("无权访问该识别会话"));
                    emitter.complete();
                    return;
                }

                final java.util.HashSet<String> seen = new java.util.HashSet<>();

                if (pending.mode == StreamMode.TEXT_CHUNKS) {
                    for (TextChunk chunk : pending.chunks) {
                        if (cancelled.get()) break;
                        try {
                            emitter.send(SseEmitter.event().name("progress").data(objectMapper.writeValueAsString(
                                    Map.of(
                                            "type", "text",
                                            "index", chunk.index,
                                            "total", chunk.total,
                                            "pageFrom", chunk.pageFrom,
                                            "pageTo", chunk.pageTo
                                    )
                            )));
                        } catch (Exception ignored) {}

                        dashScopeService.parseFromTextStream(chunk.text, draft -> {
                            try {
                                if (cancelled.get()) return;
                                String key = normalizeKey(draft);
                                if (!seen.add(key)) return;
                                emitter.send(SseEmitter.event()
                                        .name("question")
                                        .data(objectMapper.writeValueAsString(draft)));
                            } catch (IllegalStateException e) {
                                cancelled.set(true);
                            } catch (Exception e) {
                                cancelled.set(true);
                            }
                        }, cancelled);
                    }
                } else if (pending.mode == StreamMode.IMAGES) {
                    int total = pending.images.size();
                    for (int i = 0; i < total; i++) {
                        if (cancelled.get()) break;
                        int idx = i + 1;
                        try {
                            emitter.send(SseEmitter.event().name("progress").data(objectMapper.writeValueAsString(
                                    Map.of("type", "image", "index", idx, "total", total)
                            )));
                        } catch (Exception ignored) {}

                        String b64 = pending.images.get(i);
                        dashScopeService.parseFromImageStream(b64, draft -> {
                            try {
                                if (cancelled.get()) return;
                                String key = normalizeKey(draft);
                                if (!seen.add(key)) return;
                                emitter.send(SseEmitter.event()
                                        .name("question")
                                        .data(objectMapper.writeValueAsString(draft)));
                            } catch (IllegalStateException e) {
                                cancelled.set(true);
                            } catch (Exception e) {
                                cancelled.set(true);
                            }
                        }, cancelled);
                    }
                }

                if (!cancelled.get()) {
                    emitter.send(SseEmitter.event().name("done").data(""));
                }
                emitter.complete();
            } catch (Exception e) {
                log.error("SSE parse failed", e);
                try {
                    emitter.send(SseEmitter.event().name("server_error").data(e.getMessage()));
                } catch (Exception ignored) {}
                // SSE 已经开始写响应，completeWithError 可能触发容器转发 /error，
                // 进而被 Spring Security 拦截导致 response already committed。
                emitter.complete();
            }
        });
        return emitter;
    }

    private String extractTextFromFile(String fileBase64, String fileType) throws Exception {
        // MIME_BASE64 解码器兼容含换行符的 base64 字符串
        byte[] bytes = Base64.getMimeDecoder().decode(fileBase64);

        if ("txt".equalsIgnoreCase(fileType)) {
            // 自动检测编码：优先 UTF-8，失败则降级到 GBK（兼容 Windows 中文 txt）
            String text = tryDecode(bytes, StandardCharsets.UTF_8);
            if (text == null) {
                text = tryDecode(bytes, Charset.forName("GBK"));
            }
            if (text == null) {
                text = new String(bytes, StandardCharsets.UTF_8);
            }
            return text;
        }

        if ("docx".equalsIgnoreCase(fileType)) {
            try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(bytes))) {
                return doc.getParagraphs().stream()
                        .map(XWPFParagraph::getText)
                        .filter(t -> !t.isBlank())
                        .collect(Collectors.joining("\n"));
            }
        }

        if ("pdf".equalsIgnoreCase(fileType)) {
            try (PDDocument doc = Loader.loadPDF(
                    new RandomAccessReadBuffer(new ByteArrayInputStream(bytes)))) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(doc);
            }
        }

        throw new IllegalArgumentException("不支持的文件类型：" + fileType);
    }

    private PendingStream buildPendingStream(String owner, AiParseRequest request) throws Exception {
        if (request.getImageBase64List() != null && !request.getImageBase64List().isEmpty()) {
            List<String> images = request.getImageBase64List();
            return new PendingStream(owner, StreamMode.IMAGES, null, images, System.currentTimeMillis());
        }

        // 文件优先
        if (request.getFileBase64() != null && !request.getFileBase64().isBlank()) {
            String fileType = request.getFileType();
            byte[] bytes = Base64.getMimeDecoder().decode(request.getFileBase64());
            List<TextChunk> chunks = buildChunksFromFile(bytes, fileType);
            if (chunks.isEmpty()) throw new IllegalArgumentException("请提供题目内容（文本或文件）");
            return new PendingStream(owner, StreamMode.TEXT_CHUNKS, chunks, null, System.currentTimeMillis());
        }

        if (request.getText() != null && !request.getText().isBlank()) {
            List<TextChunk> chunks = splitTextToChunks(request.getText(), null);
            return new PendingStream(owner, StreamMode.TEXT_CHUNKS, chunks, null, System.currentTimeMillis());
        }

        throw new IllegalArgumentException("请提供题目内容（文本或文件或图片）");
    }

    private static final int CHUNK_MAX_CHARS = 12000;
    private static final int CHUNK_OVERLAP = 200;

    private List<TextChunk> splitTextToChunks(String text, Integer pageFrom) {
        List<TextChunk> out = new ArrayList<>();
        if (text == null) return out;
        String t = text.trim();
        if (t.isEmpty()) return out;
        int start = 0;
        while (start < t.length()) {
            int end = Math.min(t.length(), start + CHUNK_MAX_CHARS);
            String part = t.substring(start, end);
            out.add(new TextChunk(part, out.size() + 1, -1, pageFrom, null));
            if (end >= t.length()) break;
            start = Math.max(0, end - CHUNK_OVERLAP);
        }
        // 补 total
        int total = out.size();
        List<TextChunk> fixed = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            TextChunk c = out.get(i);
            fixed.add(new TextChunk(c.text, i + 1, total, c.pageFrom, c.pageTo));
        }
        return fixed;
    }

    private List<TextChunk> buildChunksFromFile(byte[] bytes, String fileType) throws Exception {
        if ("txt".equalsIgnoreCase(fileType)) {
            String text = tryDecode(bytes, StandardCharsets.UTF_8);
            if (text == null) text = tryDecode(bytes, Charset.forName("GBK"));
            if (text == null) text = new String(bytes, StandardCharsets.UTF_8);
            return splitTextToChunks(text, null);
        }

        if ("docx".equalsIgnoreCase(fileType)) {
            try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(bytes))) {
                String text = doc.getParagraphs().stream()
                        .map(XWPFParagraph::getText)
                        .filter(t -> !t.isBlank())
                        .collect(Collectors.joining("\n"));
                return splitTextToChunks(text, null);
            }
        }

        if ("pdf".equalsIgnoreCase(fileType)) {
            return splitPdfToChunks(bytes);
        }

        throw new IllegalArgumentException("不支持的文件类型：" + fileType);
    }

    private List<TextChunk> splitPdfToChunks(byte[] bytes) throws Exception {
        List<TextChunk> chunks = new ArrayList<>();
        try (PDDocument doc = Loader.loadPDF(new RandomAccessReadBuffer(new ByteArrayInputStream(bytes)))) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            int totalPages = doc.getNumberOfPages();
            StringBuilder buf = new StringBuilder();
            int startPage = 1;

            for (int p = 1; p <= totalPages; p++) {
                stripper.setStartPage(p);
                stripper.setEndPage(p);
                String pageText = stripper.getText(doc);
                if (pageText == null) pageText = "";
                String marker = "\n\n---- Page " + p + " ----\n\n";
                String toAppend = marker + pageText.trim();

                if (buf.length() + toAppend.length() > CHUNK_MAX_CHARS && buf.length() > 0) {
                    String text = buf.toString();
                    chunks.add(new TextChunk(text, chunks.size() + 1, -1, startPage, p - 1));
                    // overlap：保留末尾一段
                    String tail = text.length() > CHUNK_OVERLAP ? text.substring(text.length() - CHUNK_OVERLAP) : text;
                    buf.setLength(0);
                    buf.append(tail);
                    startPage = Math.max(1, p - 1);
                }
                buf.append(toAppend);
            }

            if (buf.length() > 0) {
                chunks.add(new TextChunk(buf.toString(), chunks.size() + 1, -1, startPage, totalPages));
            }
        }

        int total = chunks.size();
        List<TextChunk> fixed = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            TextChunk c = chunks.get(i);
            fixed.add(new TextChunk(c.text, i + 1, total, c.pageFrom, c.pageTo));
        }
        return fixed;
    }

    private String normalizeKey(QuestionDraft draft) {
        if (draft == null) return "";
        String type = draft.getType() == null ? "" : draft.getType().trim().toLowerCase();
        String content = draft.getContent() == null ? "" : draft.getContent().trim().replaceAll("\\s+", " ");
        return type + "|" + content;
    }

    /**
     * 尝试用指定编码解码字节数组，若解码后再编码回来与原始不一致则返回 null（说明编码不匹配）
     */
    private String tryDecode(byte[] bytes, Charset charset) {
        try {
            String text = new String(bytes, charset);
            // 验证：重新编码后字节数组应与原始一致
            if (java.util.Arrays.equals(text.getBytes(charset), bytes)) {
                return text;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
