package com.nfu.jasmine.flower;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 商品服务启动类。
 * <p>
 * 负责花卉主数据 CRUD、花卉状态管理。
 */
@SpringBootApplication(scanBasePackages = "com.nfu.jasmine")
@MapperScan({"com.nfu.jasmine.flower.persistence.mapper",
		"com.nfu.jasmine.infra.outbox.persistence.mapper"})
public class ProductApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProductApplication.class, args);
	}
}
