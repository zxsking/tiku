package com.example.questionbank.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CategoryVO {
    private Integer id;
    private String name;
    private Integer parentId;
    private Integer sort;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CategoryVO> children; // 子分类
}