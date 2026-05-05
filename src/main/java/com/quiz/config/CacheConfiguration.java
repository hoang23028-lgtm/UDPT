package com.quiz.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfiguration {

    public static final String LEADERBOARD_CACHE = "leaderboards";
    public static final String APP_SETTINGS_CACHE = "appSettings";
    public static final Duration LEADERBOARD_TTL = Duration.ofSeconds(60);
    public static final Duration APP_SETTINGS_TTL = Duration.ofSeconds(5);

    /**
     * Redis-backed cache: cache-aside TTL 60 seconds per cache entry.
     */
    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        RedisCacheConfiguration cacheDefaults = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(LEADERBOARD_TTL)
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer(objectMapper.copy())));

        RedisCacheConfiguration appSettings = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(APP_SETTINGS_TTL)
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new GenericJackson2JsonRedisSerializer(objectMapper.copy())));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheDefaults)
                .withInitialCacheConfigurations(Map.of(APP_SETTINGS_CACHE, appSettings))
                .transactionAware()
                .build();
    }

    /**
     * In-memory fallback when Redis is not available (e.g. tests with Redis auto-config excluded).
     */
    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public CacheManager simpleCacheManager() {
        return new ConcurrentMapCacheManager(LEADERBOARD_CACHE, APP_SETTINGS_CACHE);
    }
}
