package com.example.questionbank.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class AddCommentRequest {
    @NotNull(message = "题目ID不能为空")
    private Integer questionId;
    
    @NotBlank(message = "评论内容不能为空")
    private String content;
    
    private Integer parentId;  // 回复的目标评论ID，为空表示一级评论
}