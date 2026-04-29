package com.example.questionbank.controller;

import com.example.questionbank.common.Result;
import com.example.questionbank.service.CategoryService;
import com.example.questionbank.entity.Category;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "分类模块")
@RestController
@RequestMapping("/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @Operation(summary = "获取分类树")
    @GetMapping
    public Result<List<Category>> getCategories() {
        List<Category> categories = categoryService.getAllCategories();
        return Result.success(categories);
    }

    @Operation(summary = "创建分类")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public Result<Category> createCategory(@RequestBody Category category) {
        Category createdCategory = categoryService.createCategory(category);
        return Result.success(createdCategory);
    }

    @Operation(summary = "更新分类")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public Result<Category> updateCategory(@PathVariable Integer id, @RequestBody Category category) {
        Category updatedCategory = categoryService.updateCategory(id, category);
        return Result.success(updatedCategory);
    }

    @Operation(summary = "删除分类")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public Result<String> deleteCategory(@PathVariable Integer id) {
        categoryService.deleteCategory(id);
        return Result.success("删除成功");
    }
}