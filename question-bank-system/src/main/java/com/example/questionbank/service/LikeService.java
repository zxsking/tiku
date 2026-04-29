package com.example.questionbank.service;

public interface LikeService {
    boolean toggleLike(Integer userId, Integer targetId, String targetType);
    boolean isLiked(Integer userId, Integer targetId, String targetType);
}