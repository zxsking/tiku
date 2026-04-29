package com.example.questionbank.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.questionbank.entity.User;
import com.example.questionbank.dto.request.UpdateUserRequest;

public interface UserService {
    User getCurrentUserInfo(Long userId);
    User updateUserInfo(Long userId, UpdateUserRequest request);
    User updatePassword(Long userId, String oldPassword, String newPassword);
    User getUserById(Integer userId);
    IPage<User> getUsersForAdmin(Integer page, Integer size, String role, String status, String keyword);
    User updateUserStatus(Integer userId, String status);
    User updateUserRole(Integer userId, String role);
}