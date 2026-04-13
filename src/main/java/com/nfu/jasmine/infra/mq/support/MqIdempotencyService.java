package com.nfu.jasmine.infra.mq.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MqIdempotencyService {
    private static final Logger log = LoggerFactory.getLogger(MqIdempotencyService.class);

    private final Map<String, Long> localFallbackKeys = new ConcurrentHashMap<>();
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${app.mq.idempotency-ttl-hours:24}")
    private long ttlHours;

    public MqIdempotencyService(@Nullable StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public boolean markIfFirstConsume(String key) {
        Duration ttl = Duration.ofHours(ttlHours);
        if (stringRedisTemplate != null) {
            try {
                Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", ttl);
                return Boolean.TRUE.equals(success);
            } catch (Exception ex) {
                log.warn("MQ 幂等键写入 Redis 失败，使用本地兜底 key={} message={}", key, ex.getMessage());
            }
        }

        long expireAt = System.currentTimeMillis() + ttl.toMillis();
        cleanupExpiredKeys();
        return localFallbackKeys.putIfAbsent(key, expireAt) == null;
    }

    private void cleanupExpiredKeys() {
        long now = System.currentTimeMillis();
        localFallbackKeys.entrySet().removeIf(entry -> entry.getValue() <= now);
    }
}
