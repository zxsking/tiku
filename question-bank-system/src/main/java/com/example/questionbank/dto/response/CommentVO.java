package com.example.questionbank.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentVO {
    private Integer id;
    private Integer questionId;
    private Integer userId;
    private String content;
    private Integer parentId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}