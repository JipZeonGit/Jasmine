package com.nfu.jasmine.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * 网关限流配置。
 * <p>
 * 使用 Redis + RequestRateLimiter 实现，按客户端 IP 限流。
 * 限流规则通过 Nacos 配置 jasmine-gateway.yml 中的路由 filters 生效。
 * <p>
 * 配置示例（添加到 Nacos jasmine-gateway.yml 的路由 filters 中）：
 * <pre>
 * filters:
 *   - name: RequestRateLimiter
 *     args:
 *       redis-rate-limiter.replenishRate: 50
 *       redis-rate-limiter.burstCapacity: 100
 *       key-resolver: "#{@ipKeyResolver}"
 * </pre>
 */
@Configuration
public class GatewayRateLimiterConfig {

    /**
     * 按客户端 IP 限流。
     * <p>
     * 优先取 X-Forwarded-For（反向代理场景），否则取 RemoteAddress。
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String clientIp = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (clientIp != null && !clientIp.isBlank()) {
                // X-Forwarded-For 可能包含多个 IP，取第一个
                clientIp = clientIp.split(",")[0].trim();
            }
            if (clientIp == null || clientIp.isBlank()) {
                clientIp = exchange.getRequest().getRemoteAddress() != null
                        ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                        : "unknown";
            }
            return Mono.just(clientIp);
        };
    }
}
