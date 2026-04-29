package com.example.questionbank.common.exception;

public class BusinessException extends RuntimeException {
    private int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(String message) {
        super(message);
        this.code = -1;
    }

    public int getCode() {
        return code;
    }
}