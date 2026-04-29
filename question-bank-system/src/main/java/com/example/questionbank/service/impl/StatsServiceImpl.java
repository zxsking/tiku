package com.example.questionbank.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.questionbank.service.StatsService;
import com.example.questionbank.entity.Bank;
import com.example.questionbank.entity.Question;

import com.example.questionbank.entity.Category;
import com.example.questionbank.mapper.BankMapper;
import com.example.questionbank.mapper.QuestionMapper;
import com.example.questionbank.mapper.UserMapper;
import com.example.questionbank.mapper.FavoriteMapper;
import com.example.questionbank.mapper.CategoryMapper;
import com.example.questionbank.cache.StatsRedisCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;

@Slf4j
@Service
public class StatsServiceImpl implements StatsService {

    @Autowired
    private BankMapper bankMapper;
    @Autowired
    private QuestionMapper questionMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private FavoriteMapper favoriteMapper;
    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private StatsRedisCache statsRedisCache;

    @Override
    public Map<String, Object> getSystemStats() {
        Map<String, Object> cached = statsRedisCache.getSystemStats();
        if (cached != null) {
            return cached;
        }
        Map<String, Object> stats = loadSystemStatsFromDb();
        statsRedisCache.putSystemStats(stats);
        return stats;
    }

    private Map<String, Object> loadSystemStatsFromDb() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalBanks", bankMapper.countPublicPublishedBanks());

        List<Integer> publicBankIds = bankMapper.selectPublicPublishedBankIds();
        if (publicBankIds.isEmpty()) {
            stats.put("totalQuestions", 0L);
        } else {
            QueryWrapper<Question> publicQuestionQuery = new QueryWrapper<>();
            publicQuestionQuery.in("bank_id", publicBankIds)
                    .eq("status", "published");
            stats.put("totalQuestions", questionMapper.selectCount(publicQuestionQuery));
        }

        stats.put("totalUsers", userMapper.selectCount(null));
        stats.put("totalFavorites", favoriteMapper.selectCount(null));

        return stats;
    }

    @Override
    public List<Bank> getHotBanks() {
        List<Bank> cached = statsRedisCache.getHotBanks();
        if (cached != null) {
            return cached;
        }
        List<Bank> list = bankMapper.selectHotPublicBanks(10);
        statsRedisCache.putHotBanks(list);
        return list;
    }

    @Override
    public List<Question> getLatestQuestions() {
        List<Question> cached = statsRedisCache.getLatestQuestions();
        if (cached != null) {
            return cached;
        }

        log.info("未获取到latestquestion缓存");
        QueryWrapper<Question> questionQuery = new QueryWrapper<>();
        List<Integer> publicBankIds = bankMapper.selectPublicPublishedBankIds();
        if (publicBankIds.isEmpty()) {
            statsRedisCache.putLatestQuestions(java.util.Collections.emptyList());
            return java.util.Collections.emptyList();
        }

        questionQuery.in("bank_id", publicBankIds)
                .eq("status", "published")
                .orderByDesc("updated_at")
                .orderByDesc("id")
                .last("LIMIT 10");
        List<Question> list = questionMapper.selectList(questionQuery);
        statsRedisCache.putLatestQuestions(list);
        return list;
    }

    @Override
    public List<Map<String, Object>> getCategoryStats() {
        List<Map<String, Object>> cached = statsRedisCache.getCategoryStats();
        if (cached != null) {
            return cached;
        }
        List<Category> allCategories = categoryMapper.selectList(null);
        List<Map<String, Object>> result = new ArrayList<>();

        List<Category> rootCategories = allCategories.stream()
                .filter(c -> c.getParentId() == null || c.getParentId() == 0)
                .collect(Collectors.toList());

        for (Category category : rootCategories) {
            long count = bankMapper.countPublicPublishedByCategoryId(category.getId());

            Map<String, Object> item = new HashMap<>();
            item.put("id", category.getId());
            item.put("name", category.getName());
            item.put("count", count);
            result.add(item);
        }

        statsRedisCache.putCategoryStats(result);
        return result;
    }
}