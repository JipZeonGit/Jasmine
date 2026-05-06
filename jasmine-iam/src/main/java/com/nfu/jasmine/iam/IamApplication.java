package com.nfu.jasmine.iam;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * IAM 服务启动类。
 * <p>
 * 负责认证鉴权、用户管理、角色管理、菜单管理、JWT 签发/刷新/吊销、RBAC 授权。
 */
@SpringBootApplication
@MapperScan("com.nfu.jasmine.iam.persistence.mapper")
public class IamApplication {

	public static void main(String[] args) {
		SpringApplication.run(IamApplication.class, args);
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
