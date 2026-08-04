package com.nfu.jasmine.config;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * 集成测试基类，通过 Testcontainers 自动拉起 MySQL、Redis、RabbitMQ 三个容器。
 * <p>
 * 所有集成测试继承此类即可获得完整的中间件环境，容器在测试类生命周期内共享复用，
 * 测试结束后由 {@code @DirtiesContext} 清理 Spring 上下文避免状态泄漏。
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("integration")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class AbstractIntegrationTest {

    // 镜像版本必须与运行时部署（ops/docker|podman/prod/docker-compose.yml）保持一致，
    // 避免测试环境与生产行为漂移（如 Redis 7.2 vs 7.4 的命令语义差异）。
    @Container
    private static final MySQLContainer<?> MYSQL_CONTAINER = new MySQLContainer<>(DockerImageName.parse("mysql:8.4"))
            .withDatabaseName("jasmine")
            .withUsername("jasmine")
            .withPassword("jasmine");

    @Container
    private static final GenericContainer<?> REDIS_CONTAINER = new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379);

    @Container
    private static final RabbitMQContainer RABBITMQ_CONTAINER = new RabbitMQContainer(DockerImageName.parse("rabbitmq:4.2-management"));

    @DynamicPropertySource
    static void registerContainerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL_CONTAINER::getUsername);
        registry.add("spring.datasource.password", MYSQL_CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.sql.init.mode", () -> "never");
        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379));
        registry.add("spring.data.redis.repositories.enabled", () -> false);
        registry.add("app.cache.type", () -> "redis");
        registry.add("app.security.jwt-secret", () -> "jasmine-integration-jwt-secret-for-testcontainers-2026");
        registry.add("management.health.redis.enabled", () -> true);

        // RabbitMQ
        registry.add("spring.rabbitmq.host", RABBITMQ_CONTAINER::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ_CONTAINER::getAmqpPort);
        registry.add("spring.rabbitmq.username", RABBITMQ_CONTAINER::getAdminUsername);
        registry.add("spring.rabbitmq.password", RABBITMQ_CONTAINER::getAdminPassword);
        registry.add("app.mq.enabled", () -> true);
    }
}
