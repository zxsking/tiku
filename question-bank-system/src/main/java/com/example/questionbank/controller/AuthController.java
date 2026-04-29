package com.example.questionbank.controller;

import com.example.questionbank.common.Result;
import com.example.questionbank.service.AuthService;
import com.example.questionbank.service.UserService;
import com.example.questionbank.dto.request.LoginRequest;
import com.example.questionbank.dto.request.RegisterRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Tag(name = "认证模块")
@RestController
@RequestMapping("/auth")

public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<String> login(@Valid @RequestBody LoginRequest loginRequest) {
        String token = authService.login(loginRequest);
        return Result.success(token);
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        var user = authService.register(registerRequest);
        return Result.success(user);
    }
}