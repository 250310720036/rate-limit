package com.example.ratelimit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;

@Service
public class RateLimiterService {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> rateLimiterScript;

    public RateLimiterService(StringRedisTemplate redisTemplate, DefaultRedisScript<Long> rateLimiterScript) {
        this.redisTemplate = redisTemplate;
        this.rateLimiterScript = rateLimiterScript;
    }

    public boolean isAllowed(String key, int capacity, int refillRate) {
        long currentTimestamp = Instant.now().getEpochSecond();
        Long result = redisTemplate.execute(
                rateLimiterScript,
                Collections.singletonList("rate_limit:" + key),
                String.valueOf(capacity),
                String.valueOf(refillRate),
                String.valueOf(currentTimestamp),
                "1"
        );
        return result != null && result == 1L;
    }
}
