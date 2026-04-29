package com.example.questionbank.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
public class CreateQuestionRequest {
    @NotNull(message = "题库ID不能为空")
    private Integer bankId;
    
    @NotBlank(message = "题型不能为空")
    private String type;  // single, multiple, judge, fill, essay
    
    @NotBlank(message = "题目内容不能为空")
    private String content;
    
    private List<Object> options; // 题目选项
    
    @NotNull(message = "答案不能为空")
    private Object answer;
    
    private String analysis;
    
    private String difficulty;  // easy, medium, hard
    
    private String status;  // draft, pending, published, rejected
}