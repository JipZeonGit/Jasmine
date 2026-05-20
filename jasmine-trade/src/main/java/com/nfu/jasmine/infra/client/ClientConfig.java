package com.nfu.jasmine.infra.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * 服务间 HTTP 调用客户端配置。
 * <p>
 * 使用 Spring Cloud LoadBalancer 自动解析服务名到实例地址。
 * 配置连接超时 + 读取超时，防止下游服务不可用时长时间阻塞调用方线程。
 * 所有出站请求自动携带 X-Gateway-Token，让下游 InternalEndpointGuardFilter 放行。
 */
@Configuration
public class ClientConfig {

    /** 连接超时 3 秒 */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    /** 读取超时 5 秒 */
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    @Value("${app.security.gateway-shared-token:jasmine-dev-gateway-shared-token-change-me-2026}")
    private String gatewaySharedToken;

    @Bean
    public FlowerClient flowerClient(@LoadBalanced RestClient.Builder builder) {
        return createClient(builder, "http://product-service", FlowerClient.class);
    }

    @Bean
    public UserClient userClient(@LoadBalanced RestClient.Builder builder) {
        return createClient(builder, "http://iam-service", UserClient.class);
    }

    private <T> T createClient(RestClient.Builder builder, String baseUrl, Class<T> clientType) {
        RestClient restClient = builder
                .baseUrl(baseUrl)
                .requestFactory(createRequestFactory())
                .defaultHeader("X-Gateway-Token", gatewaySharedToken)
                .build();
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(
                RestClientAdapter.create(restClient)).build();
        return factory.createClient(clientType);
    }

    /**
     * 创建带超时配置的 JDK HttpClient 请求工厂（Java 21 原生实现）。
     * 连接超时配置在底层 HttpClient 上；读取超时配置在 RequestFactory 上。
     */
    private JdkClientHttpRequestFactory createRequestFactory() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return requestFactory;
    }
}
