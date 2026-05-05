package com.quiz.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Best-effort fixed-window rate limiter.
 * - Atomic via Lua script (INCR + EXPIRE on first hit)
 * - If Redis is down/unavailable -> "allow" (fails open) to avoid total outage.
 */
@Component
@RequiredArgsConstructor
public class RedisFixedWindowRateLimiter {

    private final ObjectProvider<StringRedisTemplate> redisProvider;

    private static final byte[] SCRIPT = ("""
            local current = redis.call('INCR', KEYS[1])
            if tonumber(current) == 1 then
              redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return current
            """).getBytes(StandardCharsets.UTF_8);

    public RateLimitDecision allow(String key, int windowSeconds, int maxRequests) {
        if (windowSeconds < 1 || maxRequests < 1) {
            return RateLimitDecision.allow();
        }
        StringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return RateLimitDecision.allow();
        }
        try {
            Long current = redis.execute(connection -> connection.eval(
                    SCRIPT,
                    ReturnType.INTEGER,
                    1,
                    key.getBytes(StandardCharsets.UTF_8),
                    Integer.toString(windowSeconds).getBytes(StandardCharsets.UTF_8)
            ), true);
            long n = current == null ? 0 : current;
            if (n <= maxRequests) {
                return RateLimitDecision.allow();
            }
            long retryAfterSeconds = windowSeconds;
            return RateLimitDecision.deny(retryAfterSeconds);
        } catch (DataAccessException e) {
            return RateLimitDecision.allow();
        }
    }

    public record RateLimitDecision(boolean allowed, Long retryAfterSeconds) {
        public static RateLimitDecision allow() {
            return new RateLimitDecision(true, null);
        }

        public static RateLimitDecision deny(long retryAfterSeconds) {
            return new RateLimitDecision(false, retryAfterSeconds);
        }
    }
}

