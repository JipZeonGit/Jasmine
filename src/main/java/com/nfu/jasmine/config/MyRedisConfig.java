package com.nfu.jasmine.config;

import com.nfu.jasmine.infra.cache.CacheNames;
import jakarta.annotation.Resource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@Profile("!test")
@org.springframework.cache.annotation.EnableCaching
@ConditionalOnProperty(name = "app.cache.type", havingValue = "redis", matchIfMissing = true)
public class MyRedisConfig {
    @Resource
    private RedisConnectionFactory factory;

    @Bean
    public RedisTemplate redisTemplate() {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        // 连接Redis
        redisTemplate.setConnectionFactory(factory);

        // 序列化处理键值对
        redisTemplate.setKeySerializer(new StringRedisSerializer());

        // 使用更强的 GenericJackson2JsonRedisSerializer 处理复杂类型序列化与反序列化
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
        redisTemplate.setValueSerializer(serializer);

        return redisTemplate;
    }

    @Bean
    public org.springframework.data.redis.cache.RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        org.springframework.data.redis.cache.RedisCacheConfiguration config = org.springframework.data.redis.cache.RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        Map<String, org.springframework.data.redis.cache.RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        // 用户、菜单、角色属于系统基础数据，允许缓存时间略长一些。
        cacheConfigurations.put(CacheNames.USER, config.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put(CacheNames.MENU_LIST, config.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put(CacheNames.ROLE_LIST, config.entryTtl(Duration.ofMinutes(30)));
        // 花卉主数据会被库存和销售联动修改，缓存时间保持更短，主要依赖显式失效保证一致性。
        cacheConfigurations.put(CacheNames.FLOWER_LIST, config.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put(CacheNames.FLOWER_DETAIL, config.entryTtl(Duration.ofMinutes(10)));

        return org.springframework.data.redis.cache.RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
