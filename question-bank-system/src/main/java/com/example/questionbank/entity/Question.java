package com.example.questionbank.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "questions", autoResultMap = true)
public class Question {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("bank_id")
    private Integer bankId;

    @TableField("type")
    private String type;

    @TableField("content")
    private String content;

    @TableField(value = "options", typeHandler = JacksonTypeHandler.class)
    private List<Object> options;

    @TableField(value = "answer", typeHandler = JacksonTypeHandler.class)
    private Object answer;

    @TableField("analysis")
    private String analysis;

    @TableField("difficulty")
    private String difficulty;

    @TableField("author_id")
    private Integer authorId;

    @TableField("status")
    private String status;

    @TableField("view_count")
    private Integer viewCount = 0;

    @TableField("like_count")
    private Integer likeCount = 0;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String bankName;

    @TableField(exist = false)
    private String bankVisibility;

    @TableField(exist = false)
    private String author;

    @TableField(exist = false)
    @JsonProperty("isLiked")
    private Boolean isLiked;

    @TableField(exist = false)
    @JsonProperty("isFavorited")
    private Boolean isFavorited;
//
//    /**
//     * 题目可见性（预留字段，当前未参与公开/私有控制）
//     * 可用于后续实现全局隐藏单题等功能
//     */
//    @TableField("visibility")
//    private String visibility;
}