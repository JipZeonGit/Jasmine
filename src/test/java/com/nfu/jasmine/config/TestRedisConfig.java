package com.nfu.jasmine.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;

import static org.mockito.Mockito.mock;

@Configuration
@Profile("test")
@EnableCaching
public class TestRedisConfig {
    @Bean
    public RedisTemplate redisTemplate() {
        return mock(RedisTemplate.class);
    }

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("user", "menuList");
    }
}