package com.nfu.jasmine;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.nfu.jasmine.*.mapper")
public class JasmineApplication {

	public static void main(String[] args) {
		SpringApplication.run(JasmineApplication.class, args);
	}

}
