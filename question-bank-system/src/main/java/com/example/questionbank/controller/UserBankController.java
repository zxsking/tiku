package com.example.questionbank.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.questionbank.common.PageResult;
import com.example.questionbank.common.Result;
import com.example.questionbank.entity.Bank;
import com.example.questionbank.entity.Category;
import com.example.questionbank.entity.Question;
import com.example.questionbank.mapper.CategoryMapper;
import com.example.questionbank.security.JwtUtil;
import com.example.questionbank.service.BankService;
import com.example.questionbank.service.FavoriteService;
import com.example.questionbank.service.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户中心模块")
@RestController
@RequestMapping("/user")
public class UserBankController {

    @Autowired
    private BankService bankService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private JwtUtil jwtUtil;

    // -------------------- 我的题库 --------------------

    @Operation(summary = "获取我的题库列表")
    @GetMapping("/banks")
    public Result<PageResult<Bank>> getMyBanks(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String visibility) {

        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(token);

        IPage<Bank> result = bankService.getMyBanks(userId.intValue(), page, size, status, visibility);
        for (Bank bank : result.getRecords()) {
            Category category = categoryMapper.selectById(bank.getCategoryId());
            if (category != null) {
                bank.setCategoryName(category.getName());
            }
        }
        return Result.success(PageResult.of(
                result.getTotal(), (int) result.getCurrent(), (int) result.getSize(), result.getRecords()
        ));
    }

    // -------------------- 我的题目 --------------------

    @Operation(summary = "获取我的题目列表")
    @GetMapping("/questions")
    public Result<PageResult<Question>> getMyQuestions(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer bankId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String keyword) {

        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(token);

        IPage<Question> result = questionService.getMyQuestions(userId.intValue(), page, size, status, bankId, type, difficulty, keyword);
        return Result.success(PageResult.of(
                result.getTotal(), (int) result.getCurrent(), (int) result.getSize(), result.getRecords()
        ));
    }

    // -------------------- 我的收藏 --------------------

    @Operation(summary = "获取收藏的题库列表")
    @GetMapping("/favorites/banks")
    public Result<PageResult<Bank>> getFavoriteBanks(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "12") Integer size) {

        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(token);

        IPage<Bank> result = favoriteService.getFavoriteBanks(userId.intValue(), page, size);
        return Result.success(PageResult.of(
                result.getTotal(), (int) result.getCurrent(), (int) result.getSize(), result.getRecords()
        ));
    }

    @Operation(summary = "获取收藏的题目列表")
    @GetMapping("/favorites/questions")
    public Result<PageResult<Question>> getFavoriteQuestions(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {

        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(token);

        IPage<Question> result = favoriteService.getFavoriteQuestions(userId.intValue(), page, size);
        return Result.success(PageResult.of(
                result.getTotal(), (int) result.getCurrent(), (int) result.getSize(), result.getRecords()
        ));
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/favorites/{type}/{id}")
    public Result<String> removeFavorite(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable String type,
            @PathVariable Integer id) {

        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(token);

        favoriteService.toggleFavorite(userId.intValue(), id, type);
        return Result.success("已取消收藏");
    }
}