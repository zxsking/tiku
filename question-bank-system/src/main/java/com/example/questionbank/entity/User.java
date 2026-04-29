package com.example.questionbank.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("users")
public class User {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("username")
    private String username;

    @TableField("email")
    private String email;

    @TableField("password")
    @JsonIgnore
    private String password;

    @TableField("avatar")
    private String avatar;

    @TableField("bio")
    private String bio;

    @TableField("role")
    private String role;

    @TableField("status")
    private String status;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private Long bankCount;

    @TableField(exist = false)
    private Long questionCount;

    @TableField(exist = false)
    private Long favoriteCount;

    @TableField(exist = false)
    private Long followingCount;
}