package com.example.questionbank.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class WrongQuestionVO {

    private Long id;
    private Integer questionId;
    private Integer userId;

    /** 累计答错次数 */
    private Integer wrongCount;

    /** 最近答错时间 */
    private LocalDateTime lastWrongAt;

    // ---- 题目详情（JOIN questions 表） ----

    private String type;
    private String content;
    private List<Object> options;
    private String difficulty;
    private Integer bankId;
    private String bankName;

    // ---- 错题本扩展字段 ----

    /** 是否已掌握 */
    private Boolean mastered;

    /** 是否收藏 */
    private Boolean starred;

    /** 错因：careless / unknown / concept */
    private String errorReason;

    /** 下次复习时间（艾宾浩斯） */
    private LocalDateTime nextReviewAt;

    /** 已复习次数 */
    private Integer reviewCount;

    // ---- 题目完整信息（详情页使用） ----

    /** 正确答案 */
    private Object correctAnswer;

    /** 题目解析 */
    private String analysis;
}
