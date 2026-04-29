package com.example.questionbank.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class AiParseRequest {
    /** 粘贴的纯文本题目内容 */
    private String text;

    /** 文件的 Base64 编码（TXT / DOCX / PDF） */
    private String fileBase64;

    /** 文件类型：txt / docx / pdf */
    private String fileType;

    /** 图片的 Base64 编码列表（支持多张图片） */
    private List<String> imageBase64List;
}
