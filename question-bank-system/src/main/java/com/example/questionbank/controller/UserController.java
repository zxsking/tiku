package com.example.questionbank.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.questionbank.common.PageResult;
import com.example.questionbank.common.Result;
import com.example.questionbank.entity.Bank;
import com.example.questionbank.entity.Question;
import com.example.questionbank.entity.User;
import com.example.questionbank.mapper.BankMapper;
import com.example.questionbank.mapper.QuestionMapper;
import com.example.questionbank.security.JwtUtil;
import com.example.questionbank.service.UserService;
import com.example.questionbank.dto.request.UpdateUserRequest;
import com.example.questionbank.dto.request.ChangePasswordRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Tag(name = "用户模块")
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private BankMapper bankMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Operation(summary = "获取指定用户公开信息")
    @GetMapping("/{id}/profile")
    public Result<User> getUserProfile(@PathVariable Integer id) {
        User user = userService.getUserById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        user.setBankCount(bankMapper.countAuthorPublicPublishedBanks(id));

        QueryWrapper<Question> questionWrapper = new QueryWrapper<>();
        questionWrapper.eq("author_id", id).eq("status", "published");
        user.setQuestionCount(questionMapper.selectCount(questionWrapper));

        user.setPassword(null);
        user.setEmail(null);
        return Result.success(user);
    }

    @Operation(summary = "获取指定用户的公开题库列表")
    @GetMapping("/{id}/banks")
    public Result<PageResult<Bank>> getUserBanks(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "12") Integer size) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Bank> pager =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        IPage<Bank> result = bankMapper.selectAuthorPublicBanksPage(pager, id);
        return Result.success(PageResult.of(result.getTotal(), (int) result.getCurrent(), (int) result.getSize(), result.getRecords()));
    }

    @Operation(summary = "获取指定用户的公开题目列表")
    @GetMapping("/{id}/questions")
    public Result<PageResult<Question>> getUserQuestions(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "15") Integer size) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Question> pager =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        java.util.List<Integer> publicBankIds = bankMapper.selectAuthorPublicPublishedBankIds(id);

        QueryWrapper<Question> wrapper = new QueryWrapper<>();
        wrapper.eq("author_id", id).eq("status", "published");
        if (!publicBankIds.isEmpty()) {
            wrapper.in("bank_id", publicBankIds);
        } else {
            // 没有公开已发布题库时，公开题目数量为 0
            wrapper.eq("id", -1);
        }
        IPage<Question> result = questionMapper.selectPage(pager, wrapper);
        return Result.success(PageResult.of(result.getTotal(), (int) result.getCurrent(), (int) result.getSize(), result.getRecords()));
    }

    @Operation(summary = "获取当前登录用户信息")
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/info")
    public Result<?> getCurrentUserInfo(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(token);
        var userInfo = userService.getCurrentUserInfo(userId);
        return Result.success(userInfo);
    }

    @Operation(summary = "更新用户信息")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/info")
    public Result<?> updateUserInfo(@RequestHeader("Authorization") String authHeader,
                                     @Valid @RequestBody UpdateUserRequest request) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(token);
        var user = userService.updateUserInfo(userId, request);
        return Result.success(user);
    }

    @Operation(summary = "修改密码")
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/password")
    public Result<?> changePassword(@RequestHeader("Authorization") String authHeader,
                                    @Valid @RequestBody ChangePasswordRequest request) {
        String token = authHeader.replace("Bearer ", "");
        Long userId = jwtUtil.getUserIdFromToken(token);

        if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
            return Result.error("新密码与确认密码不匹配");
        }

        var user = userService.updatePassword(userId, request.getOldPassword(), request.getNewPassword());
        return Result.success(user);
    }
}