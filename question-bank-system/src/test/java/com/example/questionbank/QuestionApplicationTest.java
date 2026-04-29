package com.example.questionbank;


import com.example.questionbank.cache.StatsRedisCache;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
@Slf4j
public class QuestionApplicationTest {

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Test
    public void contextLoads(){
            // 获取数据
            String jsonData = stringRedisTemplate.opsForValue()
                    .get(StatsRedisCache.KEY_LATEST_QUESTIONS);

            // 简单输出
            System.out.println("===== Redis 获取的数据 =====");
            System.out.println(jsonData);

            // 断言
            org.junit.jupiter.api.Assertions.assertNotNull(jsonData);
    }
}
