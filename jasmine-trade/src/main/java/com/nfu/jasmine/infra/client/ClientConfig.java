package com.nfu.jasmine.infra.client;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.time.Duration;

/**
 * 服务间 HTTP 调用客户端配置。
 * <p>
 * 使用 Spring Cloud LoadBalancer 自动解析服务名到实例地址，
 * 并配置连接超时与读取超时，防止下游服务不可用时长时间阻塞调用方线程。
 */
@Configuration
public class ClientConfig {

    /** 连接超时 3 秒 */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    /** 读取超时 5 秒 */
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    @Bean
    public FlowerClient flowerClient(@LoadBalanced RestClient.Builder builder) {
        RestClient restClient = builder
                .baseUrl("http://product-service")
                .requestFactory(createRequestFactory())
                .build();
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(
                RestClientAdapter.create(restClient)).build();
        return factory.createClient(FlowerClient.class);
    }

    @Bean
    public UserClient userClient(@LoadBalanced RestClient.Builder builder) {
        RestClient restClient = builder
                .baseUrl("http://iam-service")
                .requestFactory(createRequestFactory())
                .build();
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(
                RestClientAdapter.create(restClient)).build();
        return factory.createClient(UserClient.class);
    }

    /**
     * 创建带超时配置的 JDK HttpClient 请求工厂（Java 21 原生实现）。
     */
    private JdkClientHttpRequestFactory createRequestFactory() {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return requestFactory;
    }
}
