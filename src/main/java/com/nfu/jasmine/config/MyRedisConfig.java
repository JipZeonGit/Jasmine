package com.nfu.jasmine.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import javax.annotation.Resource;

@Configuration
@org.springframework.cache.annotation.EnableCaching
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

        // 使用更强大的GenericJackson2JsonRedisSerializer来处理复杂类型包括List的序列化与反序列化
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
        redisTemplate.setValueSerializer(serializer);

        return redisTemplate;
    }
    @Bean
    public org.springframework.data.redis.cache.RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        org.springframework.data.redis.cache.RedisCacheConfiguration config = org.springframework.data.redis.cache.RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(java.time.Duration.ofMinutes(30))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        return org.springframework.data.redis.cache.RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                .build();
    }
}
