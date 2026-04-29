package com.example.questionbank.controller;

import com.example.questionbank.common.Result;
import com.example.questionbank.common.PageResult;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.questionbank.security.JwtUtil;
import com.example.questionbank.service.UserService;
import com.example.questionbank.service.QuestionService;
import com.example.questionbank.service.BankService;
import com.example.questionbank.entity.User;
import com.example.questionbank.entity.Question;
import com.example.questionbank.entity.Bank;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@Tag(name = "管理员模块")
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private QuestionService questionService;

    @Autowired
    private BankService bankService;

    @Autowired
    private JwtUtil jwtUtil;

    @Operation(summary = "分页查询用户")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public Result<PageResult<User>> getUsers(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        
        IPage<User> result = userService.getUsersForAdmin(page, size, role, status, keyword);
        PageResult<User> pageResult = PageResult.of(result.getTotal(), (int)result.getCurrent(), (int)result.getSize(), result.getRecords());
        return Result.success(pageResult);
    }

    @Operation(summary = "修改用户状态")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/users/{id}/status")
    public Result<String> updateUserStatus(@PathVariable Integer id, @RequestParam String status) {
        userService.updateUserStatus(id, status);
        return Result.success("用户状态更新成功");
    }

    @Operation(summary = "修改用户角色")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/users/{id}/role")
    public Result<String> updateUserRole(@PathVariable Integer id, @RequestParam String role) {
        userService.updateUserRole(id, role);
        return Result.success("用户角色更新成功");
    }

    @Operation(summary = "分页查询题目审核列表")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/questions")
    public Result<PageResult<Question>> getQuestionsForReview(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "pending") String status) {
        
        IPage<Question> result = questionService.getQuestionsForReview(page, size, status);
        PageResult<Question> pageResult = PageResult.of(result.getTotal(), (int)result.getCurrent(), (int)result.getSize(), result.getRecords());
        return Result.success(pageResult);
    }

    @Operation(summary = "审核题目")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/questions/{id}/status")
    public Result<String> reviewQuestion(@PathVariable Integer id, @RequestParam String status) {
        questionService.reviewQuestion(id, status);
        return Result.success("题目审核状态更新成功");
    }

    @Operation(summary = "分页查询题库审核列表")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/banks")
    public Result<PageResult<Bank>> getBanksForReview(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "pending") String status) {
        
        IPage<Bank> result = bankService.getBanksForReview(page, size, status);
        PageResult<Bank> pageResult = PageResult.of(result.getTotal(), (int)result.getCurrent(), (int)result.getSize(), result.getRecords());
        return Result.success(pageResult);
    }

    @Operation(summary = "审核题库")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/banks/{id}/status")
    public Result<String> reviewBank(@PathVariable Integer id, @RequestParam String status) {
        bankService.reviewBank(id, status);
        return Result.success("题库审核状态更新成功");
    }
}