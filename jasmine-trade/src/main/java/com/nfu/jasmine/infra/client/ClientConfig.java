package com.nfu.jasmine.infra.client;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * 服务间 HTTP 调用客户端配置。
 * <p>
 * 使用 Spring Cloud LoadBalancer 自动解析服务名到实例地址。
 */
@Configuration
public class ClientConfig {

    @Bean
    public FlowerClient flowerClient(@LoadBalanced RestClient.Builder builder) {
        RestClient restClient = builder.baseUrl("http://product-service").build();
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(
                RestClientAdapter.create(restClient)).build();
        return factory.createClient(FlowerClient.class);
    }

    @Bean
    public UserClient userClient(@LoadBalanced RestClient.Builder builder) {
        RestClient restClient = builder.baseUrl("http://iam-service").build();
        HttpServiceProxyFactory factory = HttpServiceProxyFactory.builderFor(
                RestClientAdapter.create(restClient)).build();
        return factory.createClient(UserClient.class);
    }
}
