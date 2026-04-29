package com.example.questionbank.controller;

import com.example.questionbank.common.Result;
import com.example.questionbank.service.StatsService;
import com.example.questionbank.service.UserService;
import com.example.questionbank.entity.Bank;
import com.example.questionbank.entity.Category;
import com.example.questionbank.entity.Question;
import com.example.questionbank.entity.User;
import com.example.questionbank.mapper.CategoryMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "统计模块")
@RestController
@RequestMapping("/stats")
public class StatsController {

    @Autowired
    private StatsService statsService;

    @Autowired
    private UserService userService;

    @Autowired
    private CategoryMapper categoryMapper;

    @Operation(summary = "系统统计信息")
    @GetMapping
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = statsService.getSystemStats();
        return Result.success(stats);
    }

    @Operation(summary = "热门题库")
    @GetMapping("/hot-banks")
    public Result<List<Bank>> getHotBanks() {
        List<Bank> hotBanks = statsService.getHotBanks();
        for (Bank bank : hotBanks) {
            User author = userService.getUserById(bank.getAuthorId());
            if (author != null) {
                bank.setAuthor(author.getUsername());
            }
            Category category = categoryMapper.selectById(bank.getCategoryId());
            if (category != null) {
                bank.setCategoryName(category.getName());
            }
        }
        return Result.success(hotBanks);
    }

    @Operation(summary = "最新题目（按最后更新时间，含编辑；非收藏数排序）")
    @GetMapping("/latest-questions")
    public Result<List<Question>> getLatestQuestions() throws InterruptedException {
        List<Question> latestQuestions = statsService.getLatestQuestions();
//        Thread.sleep(1000);
        return Result.success(latestQuestions);
    }

    @Operation(summary = "分类统计")
    @GetMapping("/categories")
    public Result<List<?>> getCategoryStats() {
        List<?> categoryStats = statsService.getCategoryStats();
        return Result.success(categoryStats);
    }
}