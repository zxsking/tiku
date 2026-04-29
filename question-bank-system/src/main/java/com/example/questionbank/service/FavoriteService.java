package com.example.questionbank.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.questionbank.entity.Bank;
import com.example.questionbank.entity.Question;

public interface FavoriteService {
    boolean toggleFavorite(Integer userId, Integer targetId, String targetType);
    boolean isFavorited(Integer userId, Integer targetId, String targetType);

    // 查询收藏的题库列表
    IPage<Bank> getFavoriteBanks(Integer userId, Integer page, Integer size);

    // 查询收藏的题目列表
    IPage<Question> getFavoriteQuestions(Integer userId, Integer page, Integer size);
}