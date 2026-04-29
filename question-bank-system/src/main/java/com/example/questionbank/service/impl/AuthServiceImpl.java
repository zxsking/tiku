package com.example.questionbank.service.impl;

import com.example.questionbank.service.AuthService;
import com.example.questionbank.security.JwtUtil;
import com.example.questionbank.entity.User;
import com.example.questionbank.mapper.UserMapper;
import com.example.questionbank.dto.request.LoginRequest;
import com.example.questionbank.dto.request.RegisterRequest;
import com.example.questionbank.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public String login(LoginRequest loginRequest) {
        try {
            // 使用 Security 框架进行认证
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 根据用户名查找用户并做状态校验
            User user = userMapper.selectByUsername(loginRequest.getUsername());
            if (user == null || !"active".equals(user.getStatus())) {
                throw new BusinessException("用户不存在或已被禁用");
            }

            // 生成 JWT 令牌
            return jwtUtil.generateToken(user.getId().longValue(), user.getUsername());
        } catch (BadCredentialsException e) {
            // 统一用户名/密码错误提示，不把底层英文错误暴露给前端
            throw new BusinessException("用户名或密码错误");
        }
    }

    @Override
    public User register(RegisterRequest registerRequest) {
        // 检查用户名是否已存在
        User existingUser = userMapper.selectByUsername(registerRequest.getUsername());
        if (existingUser != null) {
            throw new BusinessException("用户名已存在");
        }

        // 检查邮箱是否已存在
        User userEmail = userMapper.selectByEmail(registerRequest.getEmail());
        if (userEmail != null) {
            throw new BusinessException("邮箱已存在");
        }

        // 创建用户
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setRole("user"); // 默认角色
        user.setStatus("active"); // 注册后默认激活

        userMapper.insert(user);
        return user;
    }
}