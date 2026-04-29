package com.example.questionbank.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.questionbank.common.PageResult;
import com.example.questionbank.common.Result;
import com.example.questionbank.entity.Bank;
import com.example.questionbank.entity.Category;
import com.example.questionbank.entity.Question;
import com.example.questionbank.entity.User;
import com.example.questionbank.mapper.CategoryMapper;
import com.example.questionbank.service.BankService;
import com.example.questionbank.service.QuestionService;
import com.example.questionbank.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "搜索模块")
@RestController
@RequestMapping("/search")
public class SearchController {

    @Autowired
    private BankService bankService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private UserService userService;

    @Autowired
    private CategoryMapper categoryMapper;

    @Operation(summary = "搜索题库")
    @GetMapping("/banks")
    public Result<PageResult<Bank>> searchBanks(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "12") Integer size) {

        IPage<Bank> result = bankService.getBanks(page, size, null, keyword, null, "latest");

        for (Bank bank : result.getRecords()) {
            User author = userService.getUserById(bank.getAuthorId());
            if (author != null) bank.setAuthor(author.getUsername());
            Category category = categoryMapper.selectById(bank.getCategoryId());
            if (category != null) bank.setCategoryName(category.getName());
        }

        return Result.success(PageResult.of(
                result.getTotal(), (int) result.getCurrent(), (int) result.getSize(), result.getRecords()
        ));
    }

    @Operation(summary = "搜索题目")
    @GetMapping("/questions")
    public Result<PageResult<Question>> searchQuestions(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {

        IPage<Question> result = questionService.getQuestions(page, size, null, null, null, keyword, null, "latest");

        return Result.success(PageResult.of(
                result.getTotal(), (int) result.getCurrent(), (int) result.getSize(), result.getRecords()
        ));
    }
}
