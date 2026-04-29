package com.example.questionbank.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BankVO {
    private Integer id;
    private String name;
    private String description;
    private Integer categoryId;
    private Integer authorId;
    private String visibility;
    private String status;
    private Integer questionCount;
    private Integer favoriteCount;
    private Integer viewCount;
    private Integer likeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}