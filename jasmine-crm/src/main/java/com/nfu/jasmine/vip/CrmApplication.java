package com.nfu.jasmine.vip;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 客户关系服务启动类。
 * <p>
 * 负责会员管理、预约管理（含延时提醒）、站内通知消息中心。
 */
@EnableScheduling
@SpringBootApplication(scanBasePackages = {
		"com.nfu.jasmine.vip",
		"com.nfu.jasmine.appointment",
		"com.nfu.jasmine.infra",
		"com.nfu.jasmine.common",
		"com.nfu.jasmine.config"
})
@MapperScan({"com.nfu.jasmine.vip.persistence.mapper",
		"com.nfu.jasmine.appointment.persistence.mapper",
		"com.nfu.jasmine.infra.notification.persistence.mapper",
		"com.nfu.jasmine.infra.outbox.persistence.mapper"})
public class CrmApplication {

	public static void main(String[] args) {
		SpringApplication.run(CrmApplication.class, args);
	}
}
