package com.example.questionbank.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@TableName(value = "practice_sessions", autoResultMap = true)
public class PracticeSession {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Integer userId;

    @TableField("bank_id")
    private Integer bankId;

    @TableField(value = "question_ids", typeHandler = JacksonTypeHandler.class)
    private List<Integer> questionIds;

    @TableField(value = "user_answers", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> userAnswers;

    @TableField("score")
    private Integer score;

    @TableField("total_score")
    private Integer totalScore;

    @TableField("time_limit")
    private Integer timeLimit;

    @TableField("time_used")
    private Integer timeUsed;

    @TableField("status")
    private Integer status;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField("submitted_at")
    private LocalDateTime submittedAt;
}
