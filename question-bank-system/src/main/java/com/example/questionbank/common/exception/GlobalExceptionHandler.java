package com.example.questionbank.common.exception;

import com.example.questionbank.common.Result;
import com.example.questionbank.common.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        logger.error("业务异常: {}", e.getMessage(), e);
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e) {
        String errorMsg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        logger.error("参数校验异常: {}", errorMsg);
        return Result.error(ResultCode.PARAM_ERROR.getCode(), errorMsg);
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String errorMsg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        logger.error("参数绑定异常: {}", errorMsg);
        return Result.error(ResultCode.PARAM_ERROR.getCode(), errorMsg);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Result<Void> handleAccessDeniedException(AccessDeniedException e) {
        logger.error("访问被拒绝: {}", e.getMessage());
        return Result.error(HttpStatus.FORBIDDEN.value(), "没有权限执行该操作");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnexpectedException(Exception e) {
        logger.error("系统异常: ", e);
        // 开发阶段返回具体异常信息，便于排查
        String msg = e.getMessage() != null ? e.getMessage() : ResultCode.SERVER_ERROR.getMessage();
        return Result.error(ResultCode.SERVER_ERROR.getCode(), msg);
    }
}