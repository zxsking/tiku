package com.example.questionbank.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.questionbank.entity.Bank;
import com.example.questionbank.entity.Favorite;
import com.example.questionbank.entity.Question;
import com.example.questionbank.mapper.BankMapper;
import com.example.questionbank.mapper.FavoriteMapper;
import com.example.questionbank.mapper.QuestionMapper;
import com.example.questionbank.service.FavoriteService;
import com.example.questionbank.cache.StatsRedisCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class FavoriteServiceImpl implements FavoriteService {

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private BankMapper bankMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private StatsRedisCache statsRedisCache;

    @Override
    public boolean toggleFavorite(Integer userId, Integer targetId, String targetType) {
        QueryWrapper<Favorite> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("target_type", targetType)
                .eq("target_id", targetId);

        Favorite existingFavorite = favoriteMapper.selectOne(wrapper);
        if (existingFavorite == null) {
            Favorite favorite = new Favorite();
            favorite.setUserId(userId);
            favorite.setTargetType(targetType);
            favorite.setTargetId(targetId);
            favoriteMapper.insert(favorite);
            statsRedisCache.evictAll();
            return true;
        } else {
            favoriteMapper.delete(wrapper);
            statsRedisCache.evictAll();
            return false;
        }
    }

    @Override
    public boolean isFavorited(Integer userId, Integer targetId, String targetType) {
        QueryWrapper<Favorite> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("target_type", targetType)
                .eq("target_id", targetId);
        return favoriteMapper.selectCount(wrapper) > 0;
    }

    @Override
    public IPage<Bank> getFavoriteBanks(Integer userId, Integer page, Integer size) {
        QueryWrapper<Favorite> favoriteWrapper = new QueryWrapper<>();
        favoriteWrapper.eq("user_id", userId)
                .eq("target_type", "bank")
                .orderByDesc("created_at");

        List<Favorite> favorites = favoriteMapper.selectList(favoriteWrapper);

        if (favorites.isEmpty()) {
            Page<Bank> emptyPage = new Page<>(page, size, 0);
            emptyPage.setRecords(Collections.emptyList());
            return emptyPage;
        }

        List<Integer> allIds = favorites.stream().map(Favorite::getTargetId).distinct().collect(Collectors.toList());
        List<Bank> loaded = bankMapper.selectBatchIds(allIds);
        Map<Integer, Bank> bankMap = loaded.stream().collect(Collectors.toMap(Bank::getId, b -> b));

        // 仅展示：公开且已发布，或当前用户为题库作者（与题目收藏列表逻辑一致，避免他人通过收藏看到私密题库）
        List<Bank> orderedVisible = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        for (Favorite f : favorites) {
            int bid = f.getTargetId();
            if (seen.contains(bid)) {
                continue;
            }
            Bank b = bankMap.get(bid);
            if (b == null) {
                continue;
            }
            boolean pub = "public".equals(b.getVisibility()) && "published".equals(b.getStatus());
            boolean owner = b.getAuthorId() != null && b.getAuthorId().equals(userId);
            if (pub || owner) {
                orderedVisible.add(b);
                seen.add(bid);
            }
        }

        int total = orderedVisible.size();
        int fromIndex = (page - 1) * size;
        if (fromIndex >= total) {
            Page<Bank> emptyPage = new Page<>(page, size, total);
            emptyPage.setRecords(Collections.emptyList());
            return emptyPage;
        }
        int toIndex = Math.min(fromIndex + size, total);
        List<Bank> pageRecords = orderedVisible.subList(fromIndex, toIndex);

        Page<Bank> resultPage = new Page<>(page, size, total);
        resultPage.setRecords(pageRecords);
        return resultPage;
    }

    @Override
    public IPage<Question> getFavoriteQuestions(Integer userId, Integer page, Integer size) {
        // 1. 查出该用户收藏的所有 question 类型的记录，按收藏时间倒序
        QueryWrapper<Favorite> favoriteWrapper = new QueryWrapper<>();
        favoriteWrapper.eq("user_id", userId)
                .eq("target_type", "question")
                .orderByDesc("created_at");

        List<Favorite> favorites = favoriteMapper.selectList(favoriteWrapper);

        if (favorites.isEmpty()) {
            Page<Question> emptyPage = new Page<>(page, size, 0);
            emptyPage.setRecords(Collections.emptyList());
            return emptyPage;
        }

        // 2. 提取 target_id 列表
        List<Integer> questionIds = favorites.stream()
                .map(Favorite::getTargetId)
                .collect(Collectors.toList());

        // 3. 手动分页截取
        int total = questionIds.size();
        int fromIndex = (page - 1) * size;
        if (fromIndex >= total) {
            Page<Question> emptyPage = new Page<>(page, size, total);
            emptyPage.setRecords(Collections.emptyList());
            return emptyPage;
        }
        int toIndex = Math.min(fromIndex + size, total);
        List<Integer> pageIds = questionIds.subList(fromIndex, toIndex);

        // 4. 批量查询题目
        QueryWrapper<Question> questionWrapper = new QueryWrapper<>();
        questionWrapper.in("id", pageIds);
        List<Question> questions = questionMapper.selectList(questionWrapper);

        if (questions.isEmpty()) {
            Page<Question> emptyPage = new Page<>(page, size, total);
            emptyPage.setRecords(Collections.emptyList());
            return emptyPage;
        }

        // 4.1 批量查询题库信息，用于根据题库可见性过滤题目
        List<Integer> bankIds = questions.stream()
                .map(Question::getBankId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        java.util.List<Bank> banks = bankMapper.selectBatchIds(bankIds);
        java.util.Map<Integer, Bank> bankMap = banks.stream()
                .collect(Collectors.toMap(Bank::getId, b -> b));

        // 5. 先过滤：仅保留所属题库为公开且已发布，或当前用户为题库作者的题目
        List<Question> visibleQuestions = questions.stream()
                .filter(q -> {
                    Bank bank = bankMap.get(q.getBankId());
                    if (bank == null) {
                        return false;
                    }
                    boolean isPublicPublished = "public".equals(bank.getVisibility()) && "published".equals(bank.getStatus());
                    boolean isOwner = bank.getAuthorId() != null && bank.getAuthorId().equals(userId);
                    return isPublicPublished || isOwner;
                })
                .collect(Collectors.toList());

        // 6. 按收藏顺序重新排序（基于过滤后的题目）
        List<Question> sortedQuestions = pageIds.stream()
                .map(id -> visibleQuestions.stream().filter(q -> q.getId().equals(id)).findFirst().orElse(null))
                .filter(q -> q != null)
                .collect(Collectors.toList());

        Page<Question> resultPage = new Page<>(page, size, total);
        resultPage.setRecords(sortedQuestions);
        return resultPage;
    }
}