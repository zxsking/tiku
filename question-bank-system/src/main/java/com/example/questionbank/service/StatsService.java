package com.example.questionbank.service;

import com.example.questionbank.entity.Bank;
import com.example.questionbank.entity.Question;
import java.util.List;
import java.util.Map;

public interface StatsService {
    Map<String, Object> getSystemStats();
    List<Bank> getHotBanks();
    List<Question> getLatestQuestions();
    List<?> getCategoryStats();  // 可以考虑为此创建特定的DTO
}