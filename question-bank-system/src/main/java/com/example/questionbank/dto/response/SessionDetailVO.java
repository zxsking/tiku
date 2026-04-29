package com.example.questionbank.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class SessionDetailVO {

    private Long id;
    private Integer userId;
    private Integer bankId;
    private String bankName;

    /** 时间限制（秒） */
    private Integer timeLimit;

    /** 实际用时（秒） */
    private Integer timeUsed;

    /** 0=进行中 1=已提交 */
    private Integer status;

    private LocalDateTime createdAt;
    private LocalDateTime submittedAt;

    // ---- 以下字段仅在 status=1（已提交）时返回 ----

    private Integer score;
    private Integer totalScore;
    private Double accuracy;
    private Integer correctCount;
    private Integer wrongCount;
    private Integer skipCount;

    /** 题目列表（含判分结果，仅已提交时含答案和解析） */
    private List<QuestionResultItem> questions;

    @Data
    public static class QuestionResultItem {
        private Integer id;
        private String type;
        private String content;
        private List<Object> options;
        private String difficulty;

        /** 用户作答（未提交时为 null） */
        private Object userAnswer;

        /** 正确答案（仅已提交时返回） */
        private Object correctAnswer;

        /** 题目解析（仅已提交时返回） */
        private String analysis;

        /** 是否答对（仅已提交时返回） */
        private Boolean isCorrect;
    }
}
