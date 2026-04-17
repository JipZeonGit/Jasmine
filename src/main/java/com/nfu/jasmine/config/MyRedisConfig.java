package com.nfu.jasmine.config;

import com.nfu.jasmine.infra.cache.CacheNames;
import jakarta.annotation.Resource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

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
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        // 用户、菜单、角色属于系统基础数据，基础 TTL 30 分钟，再随机加 0-5 分钟抖动，避免同批 key 一起过期。
        cacheConfigurations.put(CacheNames.USER, withJitter(config, Duration.ofMinutes(30), 300));
        cacheConfigurations.put(CacheNames.MENU_LIST, withJitter(config, Duration.ofMinutes(30), 300));
        cacheConfigurations.put(CacheNames.ROLE_LIST, withJitter(config, Duration.ofMinutes(30), 300));
        // 花卉主数据会被库存和销售联动修改，基础 TTL 10 分钟，再随机加 0-2 分钟抖动，降低集中回源。
        cacheConfigurations.put(CacheNames.FLOWER_LIST, withJitter(config, Duration.ofMinutes(10), 120));
        cacheConfigurations.put(CacheNames.FLOWER_DETAIL, withJitter(config, Duration.ofMinutes(10), 120));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(config)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    private RedisCacheConfiguration withJitter(RedisCacheConfiguration baseConfig, Duration ttl, long maxJitterSeconds) {
        return baseConfig.entryTtl((RedisCacheWriter.TtlFunction) (key, value) -> {
            long jitterSeconds = maxJitterSeconds <= 0 ? 0 : ThreadLocalRandom.current().nextLong(maxJitterSeconds + 1);
            return ttl.plusSeconds(jitterSeconds);
        });
    }
}
