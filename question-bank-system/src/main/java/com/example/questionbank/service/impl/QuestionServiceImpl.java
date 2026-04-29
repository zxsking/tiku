package com.example.questionbank.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.questionbank.cache.QueryCacheService;
import com.example.questionbank.cache.StatsRedisCache;
import com.example.questionbank.common.exception.BusinessException;
import com.example.questionbank.dto.request.CreateQuestionRequest;
import com.example.questionbank.entity.*;
import com.example.questionbank.mapper.BankMapper;
import com.example.questionbank.mapper.FavoriteMapper;
import com.example.questionbank.mapper.LikeMapper;
import com.example.questionbank.mapper.QuestionMapper;
import com.example.questionbank.service.QuestionService;
import com.example.questionbank.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@Transactional
public class QuestionServiceImpl implements QuestionService {
    private static final String QUESTION_LIST_CACHE_PREFIX = "q:list:v1";
    private static final String QUESTION_DETAIL_CACHE_PREFIX = "q:detail:v1";
    private static final Duration QUESTION_LIST_TTL = Duration.ofSeconds(90);
    private static final Duration QUESTION_LIST_EMPTY_TTL = Duration.ofSeconds(30);
    private static final Duration QUESTION_DETAIL_TTL = Duration.ofMinutes(30);

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private BankMapper bankMapper;

    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private LikeMapper likeMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private StatsRedisCache statsRedisCache;

    @Autowired
    private QueryCacheService queryCacheService;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public IPage<Question> getQuestions(Integer page, Integer size, Integer bankId, String type, String difficulty, String keyword) {
        return getQuestions(page, size, bankId, type, difficulty, keyword, null, "latest");
    }

    @Override
    public IPage<Question> getQuestions(Integer page, Integer size, Integer bankId, String type, String difficulty, String keyword, Integer currentUserId) {
        return getQuestions(page, size, bankId, type, difficulty, keyword, currentUserId, "latest");
    }

    @Override
    public IPage<Question> getQuestions(Integer page, Integer size, Integer bankId, String type, String difficulty, String keyword, Integer currentUserId, String sortBy) {
        String userScope = currentUserId == null ? "public" : "user:" + currentUserId;
        String cacheKey = queryCacheService.buildKey(
                QUESTION_LIST_CACHE_PREFIX,
                page, size, bankId, type, difficulty, keyword, sortBy, userScope
        );
        Object cached = queryCacheService.get(cacheKey);
        if (cached instanceof QuestionListCacheValue cachedValue) {
            Page<Question> cachedPage = new Page<>(cachedValue.getCurrent(), cachedValue.getSize());
            cachedPage.setTotal(cachedValue.getTotal());
            cachedPage.setRecords(toQuestionRecords(cachedValue.getRecords()));
            return cachedPage;
        }

        Page<Question> pager = new Page<>(page, size);
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();

        if (bankId != null) {
            Bank bank = bankMapper.selectById(bankId);
            if (bank == null) {
                return pager;
            }
            // 公开已发布题库所有人可查；非公开或未发布题库仅作者 / 管理员可查
            if (!canAccessBankQuestions(bank, currentUserId)) {
                return pager;
            }
            queryWrapper.eq("bank_id", bankId);
        } else {
            // 未指定题库时，只查属于公开已发布题库的题目（与 BankMapper 原生条件一致）
            java.util.List<Integer> publicBankIds = bankMapper.selectPublicPublishedBankIds();
            if (publicBankIds.isEmpty()) {
                return pager;
            }
            queryWrapper.in("bank_id", publicBankIds);
        }

        if (type != null && !type.trim().isEmpty()) {
            queryWrapper.eq("type", type);
        }
        if (difficulty != null && !difficulty.trim().isEmpty()) {
            queryWrapper.eq("difficulty", difficulty);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            queryWrapper.like("content", keyword);
        }

        queryWrapper.eq("status", "published");

        String sort = sortBy == null || sortBy.isBlank() ? "latest" : sortBy.trim().toLowerCase();
        if ("hot".equals(sort) || "popular".equals(sort)) {
            queryWrapper.orderByDesc("view_count").orderByDesc("id");
        } else {
            // 最新：按更新时间（编辑题目后也会排到前面），不用创建时间、不用收藏数
            queryWrapper.orderByDesc("updated_at").orderByDesc("id");
        }

        IPage<Question> result = questionMapper.selectPage(pager, queryWrapper);
        result.getRecords().forEach(q -> {
            Bank b = bankMapper.selectById(q.getBankId());
            if (b != null) q.setBankName(b.getName());
        });

        QuestionListCacheValue value = new QuestionListCacheValue(
                result.getTotal(),
                result.getCurrent(),
                result.getSize(),
                result.getRecords()
        );
        Duration ttl = result.getRecords().isEmpty() ? QUESTION_LIST_EMPTY_TTL : QUESTION_LIST_TTL;
        queryCacheService.set(cacheKey, value, ttl);
        return result;
    }

    @Override
    public Question getQuestionById(Integer id) {
        return getQuestionById(id, null);
    }

    @Override
    public Question getQuestionById(Integer id, Integer userId) {
        String cacheKey = queryCacheService.buildKey(QUESTION_DETAIL_CACHE_PREFIX, id);
        Question cachedQuestion = getCachedQuestion(cacheKey);

        if (cachedQuestion != null) {
            fillQuestionUserFlags(cachedQuestion, id, userId);
            return cachedQuestion;
        }
        log.info("缓存不存在");
        Question question = questionMapper.selectById(id);
        if (question == null) {
            throw new BusinessException("题目不存在");
        }
        Bank bank = bankMapper.selectById(question.getBankId());
        if (bank != null) {
            // 统一通过题库可见性 + 状态 + 题目自身状态判断访问权限
            if (!canAccessQuestion(question, bank, userId)) {
                throw new BusinessException(403, "无权访问该题目");
            }
            question.setBankName(bank.getName());
        }
        // 填充作者名
        try {
            com.example.questionbank.entity.User author = userService.getUserById(question.getAuthorId());
            if (author != null) {
                question.setAuthor(author.getUsername());
            }
        } catch (Exception ignored) {}
        // 填充当前用户的点赞/收藏状态
        if (userId != null) {
            QueryWrapper<Like> likeWrapper = new QueryWrapper<>();
            likeWrapper.eq("user_id", userId).eq("target_type", "question").eq("target_id", id);
            question.setIsLiked(likeMapper.selectCount(likeWrapper) > 0);

            QueryWrapper<Favorite> favoriteWrapper = new QueryWrapper<>();
            favoriteWrapper.eq("user_id", userId).eq("target_type", "question").eq("target_id", id);
            question.setIsFavorited(favoriteMapper.selectCount(favoriteWrapper) > 0);
        } else {
            question.setIsLiked(false);
            question.setIsFavorited(false);
        }

        if (isCacheableQuestionDetail(question, bank)) {
            Question cacheCopy = objectMapper.convertValue(question, Question.class);
            cacheCopy.setIsLiked(false);
            cacheCopy.setIsFavorited(false);
            queryCacheService.set(cacheKey, cacheCopy, QUESTION_DETAIL_TTL);
        }
        return question;
    }

    @Async("viewCountExecutor")
    @Override
    public CompletableFuture<Void> updateViewCount(Integer questionId) {
        try {
            UpdateWrapper<Question> wrapper =
                    new UpdateWrapper<>();
            wrapper.eq("id", questionId).setSql("view_count = view_count + 1");
            questionMapper.update(null, wrapper);
        } catch (Exception e) {
            log.error("更新浏览量失败, questionId: {}", questionId, e);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public Question createQuestion(CreateQuestionRequest request, Integer userId) {
        Bank bank = bankMapper.selectById(request.getBankId());
        if (bank == null) {
            throw new BusinessException("题库不存在");
        }

        if (!bank.getAuthorId().equals(userId) && !"admin".equals(userService.getUserById(userId).getRole())) {
            throw new BusinessException("没有权限向该题库添加题目");
        }

        Question question = new Question();
        question.setBankId(request.getBankId());
        question.setType(normalizeType(request.getType()));
        question.setContent(request.getContent());
        question.setOptions(request.getOptions());
        question.setAnswer(request.getAnswer());
        question.setAnalysis(request.getAnalysis());
        question.setDifficulty(normalizeDifficulty(request.getDifficulty()));
        question.setAuthorId(userId);
        // 题目状态跟随所属题库的可见性决定
        String bankVisibility = bank.getVisibility();
        question.setStatus("private".equals(bankVisibility) ? "published" : "pending");
        question.setViewCount(0);
        question.setLikeCount(0);

        questionMapper.insert(question);

        // 原子自增，避免并发更新同一行时产生死锁
        bankMapper.incrementQuestionCount(request.getBankId());

        statsRedisCache.evictAll();
        queryCacheService.evictByPrefix(QUESTION_LIST_CACHE_PREFIX);
        queryCacheService.evictByPrefix(QUESTION_DETAIL_CACHE_PREFIX);
        return question;
    }

    @Override
    public Question updateQuestion(Integer id, CreateQuestionRequest request, Integer userId) {
        Question question = questionMapper.selectById(id);
        if (question == null) {
            throw new BusinessException("题目不存在");
        }

        if (!question.getAuthorId().equals(userId) && !"admin".equals(userService.getUserById(userId).getRole())) {
            throw new BusinessException("没有权限修改此题目");
        }

        question.setType(normalizeType(request.getType()));
        question.setContent(request.getContent());
        question.setOptions(request.getOptions());
        question.setAnswer(request.getAnswer());
        question.setAnalysis(request.getAnalysis());
        question.setDifficulty(normalizeDifficulty(request.getDifficulty()));
        // 不再从请求中直接设置 status，保持现有状态不变（状态由题库可见性决定）

        questionMapper.updateById(question);
        statsRedisCache.evictAll();
        queryCacheService.evictByPrefix(QUESTION_LIST_CACHE_PREFIX);
        queryCacheService.evictByPrefix(QUESTION_DETAIL_CACHE_PREFIX);
        return question;
    }

    @Override
    public void deleteQuestion(Integer id, Integer userId) {
        Question question = questionMapper.selectById(id);
        if (question == null) {
            throw new BusinessException("题目不存在");
        }

        if (!question.getAuthorId().equals(userId) && !"admin".equals(userService.getUserById(userId).getRole())) {
            throw new BusinessException("没有权限删除此题目");
        }

        Bank bank = bankMapper.selectById(question.getBankId());

        QueryWrapper<Favorite> favoriteWrapper = new QueryWrapper<>();
        favoriteWrapper.eq("target_type", "question").eq("target_id", id);
        favoriteMapper.delete(favoriteWrapper);

        QueryWrapper<Like> likeWrapper = new QueryWrapper<>();
        likeWrapper.eq("target_type", "question").eq("target_id", id);
        likeMapper.delete(likeWrapper);

        questionMapper.deleteById(id);

        if (bank != null) {
            // 原子自减（SQL 内保证不低于 0）
            bankMapper.decrementQuestionCount(bank.getId());
        }
        statsRedisCache.evictAll();
        queryCacheService.evictByPrefix(QUESTION_LIST_CACHE_PREFIX);
        queryCacheService.evictByPrefix(QUESTION_DETAIL_CACHE_PREFIX);
    }


    @Override
    public void toggleLike(Integer userId, Integer targetId, String targetType) {
        if (!"question".equals(targetType)) {
            throw new BusinessException("目标类型不合法");
        }

        QueryWrapper<Like> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("target_type", targetType).eq("target_id", targetId);

        Like existingLike = likeMapper.selectOne(wrapper);
        UpdateWrapper<Question> qWrapper =
                new UpdateWrapper<>();
        if (existingLike == null) {
            Like like = new Like();
            like.setUserId(userId);
            like.setTargetType(targetType);
            like.setTargetId(targetId);
            likeMapper.insert(like);

            qWrapper.eq("id", targetId).setSql("like_count = like_count + 1");
            questionMapper.update(null, qWrapper);
        } else {
            likeMapper.delete(wrapper);

            qWrapper.eq("id", targetId).setSql("like_count = GREATEST(like_count - 1, 0)");
            questionMapper.update(null, qWrapper);
        }
        queryCacheService.evictByPrefix(QUESTION_DETAIL_CACHE_PREFIX);
    }

    @Override
    public void toggleFavorite(Integer userId, Integer targetId, String targetType) {
        if (!"question".equals(targetType)) {
            throw new BusinessException("目标类型不合法");
        }

        QueryWrapper<Favorite> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("target_type", targetType).eq("target_id", targetId);

        Favorite existingFavorite = favoriteMapper.selectOne(wrapper);
        if (existingFavorite == null) {
            Favorite favorite = new Favorite();
            favorite.setUserId(userId);
            favorite.setTargetType(targetType);
            favorite.setTargetId(targetId);
            favoriteMapper.insert(favorite);
        } else {
            favoriteMapper.delete(wrapper);
        }
        statsRedisCache.evictAll();
    }

    @Override
    public IPage<Question> getQuestionsForReview(Integer page, Integer size, String status) {
        Page<Question> pager = new Page<>(page, size);
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();

        if (status != null && !status.trim().isEmpty()) {
            queryWrapper.eq("status", status);
        } else {
            queryWrapper.eq("status", "pending");
        }

        queryWrapper.orderByDesc("id");

        IPage<Question> result = questionMapper.selectPage(pager, queryWrapper);
        for (Question q : result.getRecords()) {
            try {
                User u = userService.getUserById(q.getAuthorId());
                if (u != null) q.setAuthor(u.getUsername());
            } catch (Exception ignored) {}
            Bank bank = bankMapper.selectById(q.getBankId());
            if (bank != null) q.setBankName(bank.getName());
        }
        return result;
    }

    @Override
    public Question reviewQuestion(Integer id, String status) {
        Question question = questionMapper.selectById(id);
        if (question == null) {
            throw new BusinessException("题目不存在");
        }

        if ("rejected".equals(status)) {
            // 拒绝时题目回到私有可用状态（通过所属题库处理，这里只更新题目状态）
            question.setStatus("published");
            // 同时将所属题库回退为私有
            Bank bank = bankMapper.selectById(question.getBankId());
            if (bank != null) {
                bank.setVisibility("private");
                bank.setStatus("published");
                bankMapper.updateById(bank);
            }
        } else {
            question.setStatus(status);
        }
        questionMapper.updateById(question);
        statsRedisCache.evictAll();
        queryCacheService.evictByPrefix(QUESTION_LIST_CACHE_PREFIX);
        queryCacheService.evictByPrefix(QUESTION_DETAIL_CACHE_PREFIX);
        return question;
    }

    /**
     * 查询当前用户的题目列表，支持按状态筛选
     */
    @Override
    public IPage<Question> getMyQuestions(Integer userId, Integer page, Integer size, String status, Integer bankId, String type, String difficulty, String keyword) {
        Page<Question> pager = new Page<>(page, size);
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();

        // 只查当前用户的题目
        queryWrapper.eq("author_id", userId);

        // 按状态筛选（不传则查全部）
        if (status != null && !status.trim().isEmpty()) {
            queryWrapper.eq("status", status);
        }
        if (bankId != null) {
            queryWrapper.eq("bank_id", bankId);
        }
        if (type != null && !type.trim().isEmpty()) {
            queryWrapper.eq("type", type);
        }
        if (difficulty != null && !difficulty.trim().isEmpty()) {
            queryWrapper.eq("difficulty", difficulty);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            queryWrapper.like("content", keyword);
        }

        queryWrapper.orderByDesc("created_at");
        IPage<Question> pageResult = questionMapper.selectPage(pager, queryWrapper);
        for (Question question : pageResult.getRecords()) {
            Bank bank = bankMapper.selectById(question.getBankId());
            if (bank != null) {
                question.setBankName(bank.getName());
                question.setBankVisibility(bank.getVisibility());
            }
        }
        return pageResult;
    }

    /**
     * 将 AI 可能返回的各种题型名称统一映射为数据库 ENUM 值
     */
    private String normalizeType(String type) {
        if (type == null) return "essay";
        switch (type.trim().toLowerCase()) {
            case "single": case "单选": case "单选题": case "choice": case "single_choice": return "single";
            case "multiple": case "多选": case "多选题": case "multiple_choice": return "multiple";
            case "judge": case "判断": case "判断题": case "true_false": case "truefalse": return "judge";
            case "fill": case "填空": case "填空题": case "blank": case "fill_blank": return "fill";
            case "essay": case "简答": case "简答题": case "short_answer": case "open": return "essay";
            default: return "essay"; // 未知类型默认为简答
        }
    }

    /**
     * 将 AI 可能返回的各种难度名称统一映射为数据库值
     */
    private String normalizeDifficulty(String difficulty) {
        if (difficulty == null) return "medium";
        switch (difficulty.trim().toLowerCase()) {
            case "easy": case "简单": case "容易": return "easy";
            case "hard": case "困难": case "难": return "hard";
            default: return "medium";
        }
    }

    /**
     * 判断当前用户是否为管理员
     */
    private boolean isAdmin(Integer userId) {
        if (userId == null) {
            return false;
        }
        try {
            User user = userService.getUserById(userId);
            return user != null && "admin".equals(user.getRole());
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 判断当前用户是否可以访问某个题库下的题目列表
     * 规则：
     * - 题库公开且已发布：任何人可访问
     * - 其他情况：仅题库作者或管理员可访问
     */
    private boolean canAccessBankQuestions(Bank bank, Integer currentUserId) {
        if (bank == null) {
            return false;
        }
        boolean isPublicPublished = "public".equals(bank.getVisibility()) && "published".equals(bank.getStatus());
        if (isPublicPublished) {
            return true;
        }
        if (currentUserId == null) {
            return false;
        }
        if (currentUserId.equals(bank.getAuthorId())) {
            return true;
        }
        return isAdmin(currentUserId);
    }

    /**
     * 判断当前用户是否可以访问单个题目
     * 规则：
     * - 若所属题库公开且已发布：
     *   - 题目已发布：所有人可访问
     *   - 题目未发布：仅题目作者 / 题库作者 / 管理员可访问
     * - 若所属题库未公开或未发布：
     *   - 仅题目作者 / 题库作者 / 管理员可访问
     */
    private boolean canAccessQuestion(Question question, Bank bank, Integer userId) {
        if (question == null || bank == null) {
            return false;
        }
        boolean isPublicPublishedBank = "public".equals(bank.getVisibility()) && "published".equals(bank.getStatus());
        boolean isQuestionOwner = userId != null && userId.equals(question.getAuthorId());
        boolean isBankOwner = userId != null && userId.equals(bank.getAuthorId());
        boolean admin = isAdmin(userId);

        if (isPublicPublishedBank) {
            if ("published".equals(question.getStatus())) {
                // 公开已发布题库下的已发布题目：所有人可见
                return true;
            }
            // 未发布题目：仅作者 / 题库作者 / 管理员可见
            return userId != null && (isQuestionOwner || isBankOwner || admin);
        }

        // 非公开或未发布题库：仅作者 / 题库作者 / 管理员可见
        return userId != null && (isQuestionOwner || isBankOwner || admin);
    }

    private Question getCachedQuestion(String cacheKey) {
        Object cached = queryCacheService.get(cacheKey);
        if (cached == null) {
            return null;
        }
        Question question = cached instanceof Question
                ? (Question) cached
                : objectMapper.convertValue(cached, Question.class);
        question.setIsLiked(false);
        question.setIsFavorited(false);
        return question;
    }

    private void fillQuestionUserFlags(Question question, Integer questionId, Integer userId) {
        if (userId != null) {
            QueryWrapper<Like> likeWrapper = new QueryWrapper<>();
            likeWrapper.eq("user_id", userId).eq("target_type", "question").eq("target_id", questionId);
            question.setIsLiked(likeMapper.selectCount(likeWrapper) > 0);

            QueryWrapper<Favorite> favoriteWrapper = new QueryWrapper<>();
            favoriteWrapper.eq("user_id", userId).eq("target_type", "question").eq("target_id", questionId);
            question.setIsFavorited(favoriteMapper.selectCount(favoriteWrapper) > 0);
        } else {
            question.setIsLiked(false);
            question.setIsFavorited(false);
        }
    }

    private boolean isCacheableQuestionDetail(Question question, Bank bank) {
        if (question == null || bank == null) {
            return false;
        }
        return "public".equals(bank.getVisibility())
                && "published".equals(bank.getStatus())
                && "published".equals(question.getStatus());
    }

    public static class QuestionListCacheValue {
        private long total;
        private long current;
        private long size;
        private List<?> records;

        public QuestionListCacheValue() {
        }

        public QuestionListCacheValue(long total, long current, long size, List<?> records) {
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

    private List<Question> toQuestionRecords(List<?> records) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        return records.stream()
                .map(item -> item instanceof Question ? item : objectMapper.convertValue(item, Question.class))
                .map(Question.class::cast)
                .toList();
    }
}