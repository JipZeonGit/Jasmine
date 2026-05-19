package com.nfu.jasmine.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.cloud.nacos.discovery.enabled=false",
		"spring.cloud.nacos.config.enabled=false",
		"spring.main.web-application-type=reactive"
})
class GatewayContextSmokeTest {

	@Test
	void contextLoads() {
	}
}
