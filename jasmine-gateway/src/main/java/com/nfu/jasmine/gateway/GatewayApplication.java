package com.nfu.jasmine.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Jasmine 统一网关启动类。
 * <p>
 * 阶段 2 将在此模块中实现 JWT 网关鉴权过滤器与路由转发规则。
 */
@SpringBootApplication(scanBasePackages = "com.nfu.jasmine")
public class GatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}
}
