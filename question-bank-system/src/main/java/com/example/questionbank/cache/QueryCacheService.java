package com.example.questionbank.cache;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

@Service
public class QueryCacheService {

    private static final String PLACEHOLDER = "_";
    private static final String PART_DELIMITER = "|";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${app.cache.query-enabled:true}")
    private boolean queryEnabled;

    @Value("${app.cache.redis-enabled:true}")
    private boolean redisEnabled;

    public String buildKey(String prefix, Object... parts) {
        StringBuilder sb = new StringBuilder(prefix);
        if (parts == null || parts.length == 0) {
            return sb.toString();
        }
        for (Object part : parts) {
            sb.append(PART_DELIMITER).append(normalize(part));
        }
        return sb.toString();
    }

    public Object get(String key) {
        if (!redisEnabled || !queryEnabled) {
            return null;
        }
        return redisTemplate.opsForValue().get(key);
    }

    public void set(String key, Object value, Duration ttl) {
        if (!redisEnabled || !queryEnabled) {
            return;
        }
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    public void evictByPrefix(String prefix) {
        if (!redisEnabled || !queryEnabled) {
            return;
        }
        Set<String> keys = redisTemplate.keys(prefix + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private String normalize(Object value) {
        if (value == null) {
            return PLACEHOLDER;
        }
        if (value instanceof String str) {
            String trimmed = str.trim();
            return trimmed.isEmpty() ? PLACEHOLDER : trimmed;
        }
        return String.valueOf(value);
    }
}
