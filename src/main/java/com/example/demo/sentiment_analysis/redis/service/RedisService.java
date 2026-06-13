package com.example.demo.sentiment_analysis.redis.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisService(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public <T> T get(String key, TypeReference<T> typeReference) {

        try {
            String value = redisTemplate.opsForValue().get(key);

            if (value == null) {
                return null;
            }
            return objectMapper.readValue(
                    value,
                    typeReference
            );
        } catch (Exception e) {
            log.error("Redis get failed for key={}", key, e);
            return null;
        }
    }

    public void set(String key, Object value, Long ttlSeconds) {
        try {
            String jsonValue = objectMapper.writeValueAsString(value);
            if (ttlSeconds == null) {
                redisTemplate.opsForValue().set(key, jsonValue);
            } else {
                redisTemplate.opsForValue().set(key, jsonValue, ttlSeconds, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.error("Redis set failed for key={}", key, e);
        }
    }

    public void setLong(String key, Long value, Long ttlSeconds) {
        try {

            if (ttlSeconds == null) {

                redisTemplate.opsForValue()
                        .set(key, String.valueOf(value));

            } else {

                redisTemplate.opsForValue()
                        .set(key, String.valueOf(value), ttlSeconds, TimeUnit.SECONDS);
            }

        } catch (Exception e) {
            log.error("Redis setLong failed for key={}", key, e);
        }
    }

    public Long increment(String key) {
        try {
            return redisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            log.error("Redis increment failed for key={}", key, e);
            return null;
        }
    }

    public Long decrement(String key) {
        try {
            return redisTemplate.opsForValue().decrement(key);
        } catch (Exception e) {
            log.error("Redis decrement failed for key={}", key, e);
            return null;
        }
    }

    public Long getLong(String key) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return null;
            }
            return Long.parseLong(value);
        } catch (Exception e) {
            log.error("Redis getLong failed for key={}", key, e);
            return null;
        }
    }
}
