package com.example.questionbank.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.questionbank.service.BankService;
import com.example.questionbank.service.UserService;
import com.example.questionbank.service.CategoryService;
import com.example.questionbank.entity.Bank;
import com.example.questionbank.entity.Category;
import com.example.questionbank.entity.User;
import com.example.questionbank.entity.Favorite;
import com.example.questionbank.entity.Question;
import com.example.questionbank.mapper.BankMapper;
import com.example.questionbank.mapper.CategoryMapper;
import com.example.questionbank.mapper.FavoriteMapper;
import com.example.questionbank.mapper.QuestionMapper;
import com.example.questionbank.dto.request.CreateBankRequest;
import com.example.questionbank.common.exception.BusinessException;
import com.example.questionbank.cache.QueryCacheService;
import com.example.questionbank.cache.StatsRedisCache;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class BankServiceImpl implements BankService {
    private static final String BANK_LIST_CACHE_PREFIX = "qb:list:v1";
    private static final Duration BANK_LIST_TTL = Duration.ofSeconds(120);
    private static final Duration BANK_LIST_EMPTY_TTL = Duration.ofSeconds(30);

    @Autowired
    private BankMapper bankMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private StatsRedisCache statsRedisCache;

    @Autowired
    private QueryCacheService queryCacheService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public IPage<Bank> getBanks(Integer page, Integer size, Integer categoryId, String keyword, String status, String sortBy) {
        String cacheKey = queryCacheService.buildKey(
                BANK_LIST_CACHE_PREFIX,
                page, size, categoryId, keyword, status, sortBy
        );
        Object cached = queryCacheService.get(cacheKey);
        if (cached instanceof BankListCacheValue cachedValue) {
            Page<Bank> cachedPage = new Page<>(cachedValue.getCurrent(), cachedValue.getSize());
            cachedPage.setTotal(cachedValue.getTotal());
            cachedPage.setRecords(toBankRecords(cachedValue.getRecords()));
            return cachedPage;
        }

        Page<Bank> pager = new Page<>(page, size);
        List<Integer> categoryIds = null;
        if (categoryId != null) {
            categoryIds = new ArrayList<>();
            collectCategoryIds(categoryId, categoryIds);
        }
        String kw = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        String sort = sortBy == null || sortBy.isBlank() ? "latest" : sortBy.trim().toLowerCase();
        if (!"popular".equals(sort) && !"favorites".equals(sort)) {
            sort = "latest";
        }
        IPage<Bank> result = bankMapper.selectPublicBanksPage(pager, categoryIds, kw, sort);
        result.setTotal(bankMapper.countPublicBanksPage(categoryIds, kw));
        List<Bank> safe = result.getRecords().stream()
                .filter(b -> b != null && isPublicPublishedBankRow(b))
                .collect(Collectors.toList());
        result.setRecords(safe);

        BankListCacheValue value = new BankListCacheValue(
                result.getTotal(),
                result.getCurrent(),
                result.getSize(),
                result.getRecords()
        );
        Duration ttl = result.getRecords().isEmpty() ? BANK_LIST_EMPTY_TTL : BANK_LIST_TTL;
        queryCacheService.set(cacheKey, value, ttl);
        return result;
    }

    private static boolean isPublicPublishedBankRow(Bank b) {
        String v = b.getVisibility() == null ? "" : b.getVisibility().trim().toLowerCase();
        String s = b.getStatus() == null ? "" : b.getStatus().trim().toLowerCase();
        return "public".equals(v) && "published".equals(s);
    }

    /** 递归收集指定分类及其所有子分类的 ID */
    private void collectCategoryIds(Integer categoryId, List<Integer> result) {
        result.add(categoryId);
        List<Category> children = categoryMapper.selectByParentId(categoryId);
        for (Category child : children) {
            collectCategoryIds(child.getId(), result);
        }
    }

    @Override
    public Bank getBankById(Integer id) {
        return getBankById(id, null);
    }

    @Override
    public Bank getBankById(Integer id, Integer userId) {
        Bank bank = bankMapper.selectById(id);
        if (bank == null) {
            throw new BusinessException("题库不存在");
        }
        // 私有题库或审核中的公开题库，只有作者本人可访问
        boolean isPrivate = "private".equals(bank.getVisibility());
        boolean isPending = "public".equals(bank.getVisibility()) && "pending".equals(bank.getStatus());
        if ((isPrivate || isPending) && !bank.getAuthorId().equals(userId)) {
            throw new BusinessException(403, "无权访问该题库");
        }
        return bank;
    }

    @Override
    public Bank createBank(CreateBankRequest request, Integer userId) {
        Bank bank = new Bank();
        bank.setName(request.getName());
        bank.setDescription(request.getDescription());
        bank.setCategoryId(request.getCategoryId());
        bank.setAuthorId(userId);
        String visibility = request.getVisibility() != null ? request.getVisibility() : "private";
        bank.setVisibility(visibility);
        // 私有直接发布，公开进入审核
        bank.setStatus("private".equals(visibility) ? "published" : "pending");
        bank.setQuestionCount(0);
        bank.setFavoriteCount(0);
        bank.setViewCount(0);
        bank.setLikeCount(0);

        bankMapper.insert(bank);
        statsRedisCache.evictAll();
        queryCacheService.evictByPrefix(BANK_LIST_CACHE_PREFIX);
        return bank;
    }

    @Override
    public Bank updateBank(Integer id, CreateBankRequest request, Integer userId) {
        Bank bank = bankMapper.selectById(id);
        if (bank == null) {
            throw new BusinessException("题库不存在");
        }

        User currentUser = userService.getUserById(userId);
        if (!bank.getAuthorId().equals(userId) && !"admin".equals(currentUser.getRole())) {
            throw new BusinessException("没有权限修改此题库");
        }

        bank.setName(request.getName());
        bank.setDescription(request.getDescription());
        bank.setCategoryId(request.getCategoryId());

        String newVisibility = request.getVisibility();
        if (newVisibility != null && !newVisibility.equals(bank.getVisibility())) {
            bank.setVisibility(newVisibility);
            // 可见性变更时同步更新状态
            bank.setStatus("private".equals(newVisibility) ? "published" : "pending");
        } else if (newVisibility != null) {
            bank.setVisibility(newVisibility);
        }

        bankMapper.updateById(bank);
        statsRedisCache.evictAll();
        queryCacheService.evictByPrefix(BANK_LIST_CACHE_PREFIX);
        return bank;
    }

    @Override
    public void deleteBankById(Integer id, Integer userId) {
        Bank bank = bankMapper.selectById(id);
        if (bank == null) {
            throw new BusinessException("题库不存在");
        }

        User currentUser = userService.getUserById(userId);
        if (!bank.getAuthorId().equals(userId) && !"admin".equals(currentUser.getRole())) {
            throw new BusinessException("没有权限删除此题库");
        }

        QueryWrapper<Question> questionWrapper = new QueryWrapper<>();
        questionWrapper.eq("bank_id", id);
        questionMapper.delete(questionWrapper);

        QueryWrapper<Favorite> favoriteWrapper = new QueryWrapper<>();
        favoriteWrapper.eq("target_type", "bank").eq("target_id", id);
        favoriteMapper.delete(favoriteWrapper);

        bankMapper.deleteById(id);
        statsRedisCache.evictAll();
        queryCacheService.evictByPrefix(BANK_LIST_CACHE_PREFIX);
    }

    @Override
    public void updateViewCount(Integer bankId) {
        Bank bank = bankMapper.selectById(bankId);
        if (bank != null) {
            bank.setViewCount(bank.getViewCount() + 1);
            bankMapper.updateById(bank);
        }
    }

    @Override
    public void toggleFavorite(Integer userId, Integer targetId, String targetType) {
        if (!"bank".equals(targetType)) {
            throw new BusinessException("目标类型不合法");
        }

        QueryWrapper<Favorite> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("target_type", targetType)
                .eq("target_id", targetId);

        Favorite existingFavorite = favoriteMapper.selectOne(wrapper);
        if (existingFavorite == null) {
            // 与详情页一致：未发布/私密题库仅作者可访问，防止通过直接调收藏接口窥探他人私密题库
            getBankById(targetId, userId);

            Favorite favorite = new Favorite();
            favorite.setUserId(userId);
            favorite.setTargetType(targetType);
            favorite.setTargetId(targetId);
            favoriteMapper.insert(favorite);

            Bank bank = bankMapper.selectById(targetId);
            if (bank != null) {
                bank.setFavoriteCount(bank.getFavoriteCount() + 1);
                bankMapper.updateById(bank);
            }
        } else {
            favoriteMapper.delete(wrapper);

            Bank bank = bankMapper.selectById(targetId);
            if (bank != null && bank.getFavoriteCount() > 0) {
                bank.setFavoriteCount(bank.getFavoriteCount() - 1);
                bankMapper.updateById(bank);
            }
        }
        statsRedisCache.evictAll();
        queryCacheService.evictByPrefix(BANK_LIST_CACHE_PREFIX);
    }

    @Override
    public IPage<Bank> getBanksForReview(Integer page, Integer size, String status) {
        Page<Bank> pager = new Page<>(page, size);
        QueryWrapper<Bank> queryWrapper = new QueryWrapper<>();

        if (status != null && !status.trim().isEmpty()) {
            queryWrapper.eq("status", status);
        } else {
            queryWrapper.eq("status", "pending");
        }

        queryWrapper.orderByDesc("id");

        IPage<Bank> result = bankMapper.selectPage(pager, queryWrapper);
        for (Bank b : result.getRecords()) {
            try {
                User u = userService.getUserById(b.getAuthorId());
                if (u != null) b.setAuthor(u.getUsername());
            } catch (Exception ignored) {}
        }
        return result;
    }

    @Override
    public Bank reviewBank(Integer id, String status) {
        Bank bank = bankMapper.selectById(id);
        if (bank == null) {
            throw new BusinessException("题库不存在");
        }

        if ("rejected".equals(status)) {
            // 拒绝时回退为私有可用状态
            bank.setVisibility("private");
            bank.setStatus("published");
        } else {
            bank.setStatus(status);
        }
        bankMapper.updateById(bank);
        statsRedisCache.evictAll();
        queryCacheService.evictByPrefix(BANK_LIST_CACHE_PREFIX);
        return bank;
    }

    /**
     * 查询指定用户的题库列表，支持按状态筛选
     */
    @Override
    public IPage<Bank> getMyBanks(Integer userId, Integer page, Integer size, String status) {
        return getMyBanks(userId, page, size, status, null);
    }

    @Override
    public IPage<Bank> getMyBanks(Integer userId, Integer page, Integer size, String status, String visibility) {
        Page<Bank> pager = new Page<>(page, size);
        QueryWrapper<Bank> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq("author_id", userId);

        if (status != null && !status.trim().isEmpty()) {
            queryWrapper.eq("status", status);
        }
        if (visibility != null && !visibility.trim().isEmpty()) {
            queryWrapper.eq("visibility", visibility);
        }

        queryWrapper.orderByDesc("created_at");
        return bankMapper.selectPage(pager, queryWrapper);
    }

    public static class BankListCacheValue {
        private long total;
        private long current;
        private long size;
        private List<?> records;

        public BankListCacheValue() {
        }

        public BankListCacheValue(long total, long current, long size, List<?> records) {
            this.total = total;
            this.current = current;
            this.size = size;
            this.records = records;
        }

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        public long getCurrent() {
            return current;
        }

        public void setCurrent(long current) {
            this.current = current;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public List<?> getRecords() {
            return records;
        }

        public void setRecords(List<?> records) {
            this.records = records;
        }
    }

    private List<Bank> toBankRecords(List<?> records) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        return records.stream()
                .map(item -> item instanceof Bank ? item : objectMapper.convertValue(item, Bank.class))
                .map(Bank.class::cast)
                .collect(Collectors.toList());
    }
}