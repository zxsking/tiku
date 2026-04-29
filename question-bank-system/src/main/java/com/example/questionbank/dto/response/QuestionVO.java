package com.example.questionbank.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class QuestionVO {
    private Integer id;
    private Integer bankId;
    private String type;
    private String content;
    private List<Object> options;
    private Object answer;
    private String analysis;
    private String difficulty;
    private Integer authorId;
    private String status;
    private Integer viewCount;
    private Integer likeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}