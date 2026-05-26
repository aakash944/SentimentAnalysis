package com.example.demo.sentiment_analysis.comment.service;

import com.example.demo.sentiment_analysis.redis_service.RedisService;
import org.springframework.stereotype.Service;

@Service
public class RedisCommentService {
    private final RedisService redisService;

    public RedisCommentService(RedisService redisService) {
        this.redisService = redisService;
    }

}
