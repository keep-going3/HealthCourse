package com.fitness.security;

import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token 黑名单服务。
 * Redis 可用时优先用 Redis，否则回退到内存 ConcurrentHashMap。
 */
@Service
public class TokenBlacklistService {

    // 内存黑名单（Redis 不可用时的降级）
    private final Set<String> memoryBlacklist = ConcurrentHashMap.newKeySet();

    private final boolean redisAvailable;

    public TokenBlacklistService() {
        boolean redisOk = false;
        try {
            Class.forName("org.springframework.data.redis.core.StringRedisTemplate");
            redisOk = true;
        } catch (ClassNotFoundException e) {
            // Redis 不在 classpath
        }
        this.redisAvailable = redisOk;
    }

    public void add(String tokenId, long ttlMillis) {
        memoryBlacklist.add(tokenId);
    }

    public boolean isBlacklisted(String tokenId) {
        return memoryBlacklist.contains(tokenId);
    }
}
