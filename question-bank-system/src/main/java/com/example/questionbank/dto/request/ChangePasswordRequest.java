package com.example.questionbank.dto.request;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class ChangePasswordRequest {
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;
    
    @NotBlank(message = "新密码不能为空")
    private String newPassword;
    
    @NotBlank(message = "确认新密码不能为空")
    private String confirmNewPassword;
}