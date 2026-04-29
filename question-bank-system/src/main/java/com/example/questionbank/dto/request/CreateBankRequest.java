package com.example.questionbank.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class CreateBankRequest {
    @NotBlank(message = "题库名称不能为空")
    private String name;
    
    private String description;
    
    @NotNull(message = "分类ID不能为空")
    private Integer categoryId;
    
    private String visibility; // public/private
}