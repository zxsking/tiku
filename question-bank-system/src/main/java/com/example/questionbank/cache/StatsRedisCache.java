package com.example.questionbank.cache;

import com.example.questionbank.entity.Bank;
import com.example.questionbank.entity.Question;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 首页/统计相关读多写少数据缓存，减轻数据库压力。
 * Redis 不可用时静默降级为每次查库。
 */
@Component
public class StatsRedisCache {

    private static final Logger log = LoggerFactory.getLogger(StatsRedisCache.class);

    public static final String KEY_SYSTEM_STATS = "tiku:stats:system";
    public static final String KEY_HOT_BANKS = "tiku:stats:hot-banks";
    public static final String KEY_LATEST_QUESTIONS = "tiku:stats:latest-questions";
    public static final String KEY_CATEGORY_STATS = "tiku:stats:category-stats";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.cache.stats-enabled:true}")
    private boolean statsEnabled;

    @Value("${app.cache.redis-enabled:true}")
    private boolean redisEnabled;

    @Value("${app.cache.stats-ttl-seconds:120}")
    private long statsTtlSeconds;

    private Duration ttl() {
        return Duration.ofSeconds(Math.max(5, statsTtlSeconds));
    }

    private boolean enabled() {
        return redisEnabled && statsEnabled;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getSystemStats() {
        if (!enabled()) return null;
        try {
            Object v = redisTemplate.opsForValue().get(KEY_SYSTEM_STATS);
            if (v instanceof Map) {
                return (Map<String, Object>) v;
            }
        } catch (Exception e) {
            log.warn("Redis get system stats failed: {}", e.getMessage());
        }
        return null;
    }

    public void putSystemStats(Map<String, Object> stats) {
        if (!enabled() || stats == null) return;
        try {
            redisTemplate.opsForValue().set(KEY_SYSTEM_STATS, stats, ttl());
        } catch (Exception e) {
            log.warn("Redis put system stats failed: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public List<Bank> getHotBanks() {
        if (!enabled()) return null;
        try {
            Object v = redisTemplate.opsForValue().get(KEY_HOT_BANKS);
            if (v instanceof List) {
                return convertList((List<?>) v, Bank.class);
            }
        } catch (Exception e) {
            log.warn("Redis get hot banks failed: {}", e.getMessage());
        }
        return null;
    }

    public void putHotBanks(List<Bank> banks) {
        if (!enabled() || banks == null) return;
        try {
            redisTemplate.opsForValue().set(KEY_HOT_BANKS, banks, ttl());
        } catch (Exception e) {
            log.warn("Redis put hot banks failed: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public List<Question> getLatestQuestions() {
        if (!enabled()) return null;
        try {
            Object v = redisTemplate.opsForValue().get(KEY_LATEST_QUESTIONS);
            if (v instanceof List) {
                return convertList((List<?>) v, Question.class);
            }
        } catch (Exception e) {
            log.warn("Redis get latest questions failed: {}", e.getMessage());
        }
        return null;
    }

    public void putLatestQuestions(List<Question> questions) {
        if (!enabled() || questions == null) return;
        try {
            redisTemplate.opsForValue().set(KEY_LATEST_QUESTIONS, questions, ttl());
        } catch (Exception e) {
            log.warn("Redis put latest questions failed: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getCategoryStats() {
        if (!enabled()) return null;
        try {
            Object v = redisTemplate.opsForValue().get(KEY_CATEGORY_STATS);
            if (v instanceof List) {
                return (List<Map<String, Object>>) v;
            }
        } catch (Exception e) {
            log.warn("Redis get category stats failed: {}", e.getMessage());
        }
        return null;
    }

    public void putCategoryStats(List<Map<String, Object>> list) {
        if (!enabled() || list == null) return;
        try {
            redisTemplate.opsForValue().set(KEY_CATEGORY_STATS, list, ttl());
        } catch (Exception e) {
            log.warn("Redis put category stats failed: {}", e.getMessage());
        }
    }

    /** 管理端或批量变更数据时可调用，立即失效统计缓存 */
    public void evictAll() {
        if (!enabled()) return;
        try {
            redisTemplate.delete(KEY_SYSTEM_STATS);
            redisTemplate.delete(KEY_HOT_BANKS);
            redisTemplate.delete(KEY_LATEST_QUESTIONS);
            redisTemplate.delete(KEY_CATEGORY_STATS);
        } catch (Exception e) {
            log.warn("Redis evict stats failed: {}", e.getMessage());
        }
    }

    private <T> List<T> convertList(List<?> raw, Class<T> targetClass) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        return raw.stream()
                .map(item -> targetClass.isInstance(item) ? targetClass.cast(item) : objectMapper.convertValue(item, targetClass))
                .collect(Collectors.toList());
    }
}
