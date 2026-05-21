package com.nfu.jasmine.infra.client;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * crm-service 的服务间 HTTP 客户端配置。
 * <p>
 * 声明 crm 需要调用的远程客户端 Bean，公共创建逻辑委托给 {@link InternalClientFactory}。
 */
@Configuration
public class CrmClientConfig {

    @Bean
    public CrmUserClient crmUserClient(@LoadBalanced RestClient.Builder builder, InternalClientFactory factory) {
        return factory.createClient(builder, "http://iam-service", CrmUserClient.class);
    }
}
