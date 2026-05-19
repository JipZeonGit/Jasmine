package com.nfu.jasmine.schema;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 独立数据库初始化入口。
 * <p>
 * 容器化环境先运行该模块完成 Flyway 迁移，再启动各业务服务，避免 IAM 服务承担隐式 schema owner。
 */
@SpringBootApplication
public class SchemaApplication {

	public static void main(String[] args) {
		SpringApplication.run(SchemaApplication.class, args);
	}
}
