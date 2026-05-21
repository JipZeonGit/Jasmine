package com.nfu.jasmine.infra.mq;

import org.junit.jupiter.api.Disabled;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Outbox + 库存预警集成测试 —— 待重构。
 * <p>
 * 此测试原本通过 FlowerMapper 直接向 jasmine_trade 库插入花卉记录，
 * 在 Phase 3 远程化后 trade-service 不再拥有 FlowerMapper。
 * 需要重构为：通过 FlowerClient 远程接口准备测试数据，
 * 或使用 Testcontainers 同时编排 product-service 和 trade-service。
 */
@Disabled("Phase 3 远程化后需要重构：trade 不再直接依赖 FlowerMapper")
@SpringBootTest
public class OutboxAndAlertIntegrationIT {
}
