package com.example.questionbank.dto.request;

import lombok.Data;

import java.util.Map;

@Data
public class SubmitSessionRequest {

    /**
     * 用户答案，key 为题目ID字符串，value 为答案
     * 单选/判断：String，如 "A" 或 "true"
     * 多选：List<String>，如 ["A","B"]
     */
    private Map<String, Object> userAnswers;

    /** 实际用时（秒） */
    private Integer timeUsed = 0;
}
