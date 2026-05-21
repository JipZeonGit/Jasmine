package com.nfu.jasmine.sales;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 交易服务启动类。
 * <p>
 * 负责销售单管理、库存流水管理、库存原子扣减、库存预警读模型、Outbox 事件发布。
 */
@SpringBootApplication(scanBasePackages = {
		"com.nfu.jasmine.sales",
		"com.nfu.jasmine.inventory",
		"com.nfu.jasmine.infra",
		"com.nfu.jasmine.common",
		"com.nfu.jasmine.config"
})
@EnableScheduling
@MapperScan({"com.nfu.jasmine.sales.persistence.mapper",
		"com.nfu.jasmine.inventory.persistence.mapper",
		"com.nfu.jasmine.inventory.alert.persistence.mapper",
		"com.nfu.jasmine.infra.outbox.persistence.mapper"})
public class TradeApplication {

	public static void main(String[] args) {
		SpringApplication.run(TradeApplication.class, args);
	}
}
