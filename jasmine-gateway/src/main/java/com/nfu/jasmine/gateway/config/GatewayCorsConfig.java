package com.nfu.jasmine.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 网关统一 CORS 配置。
 * <p>
 * 微服务架构下，前端只与 Gateway 通信，CORS 只需在 Gateway 配置一次。
 * 各下游服务不再需要自己的 CorsConfig。
 */
@Configuration
public class GatewayCorsConfig {

    @Value("${app.cors.allowed-origins:http://localhost:8888,http://localhost:5173,http://127.0.0.1:5173,http://localhost}")
    private List<String> allowedOrigins;

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        allowedOrigins.forEach(config::addAllowedOrigin);
        config.setAllowCredentials(true);
        config.addAllowedMethod("*");
        config.addAllowedHeader("*");
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
