package com.shop.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class SessionService {
    private final StringRedisTemplate redisTemplate;
    private final String sessionPrefix;

    public SessionService(StringRedisTemplate redisTemplate,
                          @Value("${shop.session-prefix:session:}") String sessionPrefix) {
        this.redisTemplate = redisTemplate;
        this.sessionPrefix = sessionPrefix;
    }

    public String getLoginByToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return redisTemplate.opsForValue().get(sessionPrefix + token);
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
