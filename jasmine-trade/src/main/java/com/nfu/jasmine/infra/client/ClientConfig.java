package com.nfu.jasmine.infra.client;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * trade-service 的服务间 HTTP 客户端配置。
 * <p>
 * 声明 trade 需要调用的三个远程客户端 Bean，公共创建逻辑委托给 {@link InternalClientFactory}。
 */
@Configuration
public class ClientConfig {

    @Bean
    public FlowerClient flowerClient(@LoadBalanced RestClient.Builder builder, InternalClientFactory factory) {
        return factory.createClient(builder, "http://product-service", FlowerClient.class);
    }

    @Bean
    public UserClient userClient(@LoadBalanced RestClient.Builder builder, InternalClientFactory factory) {
        return factory.createClient(builder, "http://iam-service", UserClient.class);
    }

    @Bean
    public VipClient vipClient(@LoadBalanced RestClient.Builder builder, InternalClientFactory factory) {
        return factory.createClient(builder, "http://crm-service", VipClient.class);
    }
}
