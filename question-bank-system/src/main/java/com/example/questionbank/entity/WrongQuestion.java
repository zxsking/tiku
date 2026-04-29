package com.example.questionbank.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wrong_questions")
public class WrongQuestion {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Integer userId;

    @TableField("question_id")
    private Integer questionId;

    @TableField("wrong_count")
    private Integer wrongCount;

    @TableField("last_wrong_at")
    private LocalDateTime lastWrongAt;

    /** 是否已掌握 */
    @TableField("mastered")
    private Boolean mastered;

    /** 是否收藏 */
    @TableField("starred")
    private Boolean starred;

    /** 错因：careless / unknown / concept */
    @TableField("error_reason")
    private String errorReason;

    /** 下次复习时间（艾宾浩斯） */
    @TableField("next_review_at")
    private LocalDateTime nextReviewAt;

    /** 已复习次数 */
    @TableField("review_count")
    private Integer reviewCount;
}
