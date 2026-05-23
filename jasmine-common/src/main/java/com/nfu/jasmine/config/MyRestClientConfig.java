package com.nfu.jasmine.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * 注册 @LoadBalanced RestClient.Builder Bean。
 * <p>
 * Spring Boot 3.5 + Spring Cloud 2025.0.0 不再自动注册该 Bean，
 * 需要手动声明，否则使用 @LoadBalanced RestClient.Builder 注入会报：
 * No qualifying bean of type 'org.springframework.web.client.RestClient$Builder' available
 */
@Configuration
public class MyRestClientConfig {

    @Bean
    @LoadBalanced
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
