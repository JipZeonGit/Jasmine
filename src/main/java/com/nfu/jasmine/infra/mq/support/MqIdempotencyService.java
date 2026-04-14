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

/**
 * MQ 消费端幂等服务。
 * <p>
 * 优先使用 Redis SETNX 做分布式幂等；Redis 不可用时自动降级到本地 ConcurrentHashMap 兜底，
 * 保证单节点内的短期去重能力，避免 Redis 短暂抖动直接击穿整条消费链。
 */
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

    /**
     * 尝试标记一条消息为"已消费"。返回 true 表示首次消费，false 表示重复消息应跳过。
     */
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

        // Redis 降级后走本地 Map，记录过期时间戳来模拟 TTL。
        long expireAt = System.currentTimeMillis() + ttl.toMillis();
        cleanupExpiredKeys();
        return localFallbackKeys.putIfAbsent(key, expireAt) == null;
    }

    // 每次写入前顺便清理过期键，防止本地 Map 无限膨胀。
    private void cleanupExpiredKeys() {
        long now = System.currentTimeMillis();
        localFallbackKeys.entrySet().removeIf(entry -> entry.getValue() <= now);
    }
}
