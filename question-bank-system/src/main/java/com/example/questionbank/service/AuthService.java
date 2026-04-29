package com.example.questionbank.service;

import com.example.questionbank.dto.request.LoginRequest;
import com.example.questionbank.dto.request.RegisterRequest;
import com.example.questionbank.entity.User;

public interface AuthService {
    String login(LoginRequest loginRequest);
    User register(RegisterRequest registerRequest);
}