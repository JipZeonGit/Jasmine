package com.nfu.jasmine;

import com.nfu.jasmine.config.JasmineNativeImageHints;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.nfu.jasmine.**.persistence.mapper")
@ImportRuntimeHints(JasmineNativeImageHints.class)
public class JasmineApplication {

	public static void main(String[] args) {
		SpringApplication.run(JasmineApplication.class, args);
	}

	@Bean
	public PasswordEncoder passwordEncoder(){
		return new BCryptPasswordEncoder();
	}
}

