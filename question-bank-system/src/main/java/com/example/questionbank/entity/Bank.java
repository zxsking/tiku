package com.example.questionbank.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("banks")
public class Bank {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("name")
    private String name;

    @TableField("description")
    private String description;

    @TableField("category_id")
    private Integer categoryId;

    @TableField("author_id")
    private Integer authorId;

    @TableField("visibility")
    private String visibility;

    @TableField("status")
    private String status;

    @TableField("question_count")
    private Integer questionCount = 0;

    @TableField("favorite_count")
    private Integer favoriteCount = 0;

    @TableField("view_count")
    private Integer viewCount = 0;

    @TableField("like_count")
    private Integer likeCount = 0;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String author;

    @TableField(exist = false)
    private String categoryName;

    @TableField(exist = false)
    private Long authorBankCount;

    @TableField(exist = false)
    private Boolean isFavorited;

    @TableField(exist = false)
    private Boolean isFollowed;
}