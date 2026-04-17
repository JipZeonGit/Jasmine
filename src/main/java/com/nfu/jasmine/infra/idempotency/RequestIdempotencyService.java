package com.nfu.jasmine.infra.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nfu.jasmine.common.enums.ResultCode;
import com.nfu.jasmine.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 创建类写接口请求幂等服务。
 * <p>
 * 优先使用客户端传入的 {@code X-Idempotency-Key}；如果客户端未显式传值，则退回到
 * “接口作用域 + 操作人 + 请求体”的指纹哈希，优先拦截双击、浏览器重试和短时重复提交。
 */
@Service
public class RequestIdempotencyService {
    public static final String IDEMPOTENCY_HEADER = "X-Idempotency-Key";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, Long> localFallbackKeys = new ConcurrentHashMap<>();

    @Value("${app.idempotency.create-ttl-seconds:30}")
    private long ttlSeconds;

    public RequestIdempotencyService(@Nullable StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public void executeCreate(String scope, Integer actorId, @Nullable String clientKey, Object payload, Runnable action) {
        String idempotencyKey = buildKey(scope, actorId, clientKey, payload);
        Duration ttl = Duration.ofSeconds(ttlSeconds);
        if (!claimKey(idempotencyKey, ttl)) {
            throw new BusinessException(ResultCode.CONFLICT, "请求重复提交，请稍后再试！");
        }

        boolean success = false;
        try {
            action.run();
            success = true;
        } finally {
            if (!success) {
                releaseKey(idempotencyKey);
            }
        }
    }

    private String buildKey(String scope, Integer actorId, @Nullable String clientKey, Object payload) {
        String normalizedClientKey = StringUtils.hasText(clientKey) ? clientKey.trim() : fingerprintPayload(scope, actorId, payload);
        return "jasmine:req:idempotent:" + scope + ":" + normalizedClientKey;
    }

    private String fingerprintPayload(String scope, Integer actorId, Object payload) {
        try {
            String raw = scope + '|' + actorId + '|' + objectMapper.writeValueAsString(payload);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (JsonProcessingException | NoSuchAlgorithmException ex) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "生成请求幂等键失败，请稍后重试！");
        }
    }

    private boolean claimKey(String key, Duration ttl) {
        if (stringRedisTemplate != null) {
            try {
                Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", ttl);
                return Boolean.TRUE.equals(success);
            } catch (Exception ignored) {
                // Redis 不可用时退回本地短期兜底，至少防住单节点下的重复提交。
            }
        }

        long expireAt = System.currentTimeMillis() + ttl.toMillis();
        cleanupExpiredKeys();
        return localFallbackKeys.putIfAbsent(key, expireAt) == null;
    }

    private void releaseKey(String key) {
        if (stringRedisTemplate != null) {
            try {
                stringRedisTemplate.delete(key);
                return;
            } catch (Exception ignored) {
                // Redis 删除失败时继续落到本地兜底清理。
            }
        }
        localFallbackKeys.remove(key);
    }

    private void cleanupExpiredKeys() {
        long now = System.currentTimeMillis();
        localFallbackKeys.entrySet().removeIf(entry -> entry.getValue() <= now);
    }
}
