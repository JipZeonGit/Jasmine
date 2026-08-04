package com.nfu.jasmine.infra.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * 服务间 HTTP 客户端工厂。
 * <p>
 * 封装 RestClient 创建、超时配置、网关令牌注入、traceId 传播等公共逻辑，
 * 各服务的 ClientConfig 只需声明 Bean 方法，调用 {@link #createClient} 即可。
 */
@Component
public class InternalClientFactory {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    @Value("${app.security.gateway-shared-token:jasmine-dev-gateway-shared-token-change-me-2026}")
    private String gatewaySharedToken;

    /**
     * 创建指定类型的 HTTP Interface 代理客户端。
     *
     * @param builder    LoadBalanced RestClient 构建器
     * @param baseUrl    目标服务基础 URL（如 http://product-service）
     * @param clientType HTTP Interface 接口类型
     * @return 代理实例
     */
    public <T> T createClient(RestClient.Builder builder, String baseUrl, Class<T> clientType) {
        RestClient restClient = builder
                .baseUrl(baseUrl)
                .requestFactory(createRequestFactory())
                .requestInterceptor(new TraceContextPropagatingInterceptor())
                .defaultHeader("X-Gateway-Token", gatewaySharedToken)
                .build();
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(
                RestClientAdapter.create(restClient)).build();
        return factory.createClient(clientType);
    }

    /**
     * 创建带超时配置的 JDK HttpClient 请求工厂。
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
