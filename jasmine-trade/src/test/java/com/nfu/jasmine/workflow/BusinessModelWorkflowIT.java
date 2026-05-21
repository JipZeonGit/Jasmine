package com.nfu.jasmine.workflow;

import org.junit.jupiter.api.Disabled;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 跨服务业务流程集成测试 —— 待重构。
 * <p>
 * 此测试原本依赖 jasmine-product 和 jasmine-crm 的本地 Mapper 和 Entity，
 * 在 Phase 3 远程化后这些依赖已移除。需要拆分为：
 * 1. trade-service 内部的纯本域集成测试
 * 2. 跨服务端到端测试（通过远程接口或 Testcontainers 多服务编排）
 */
@Disabled("Phase 3 远程化后需要重构：trade 不再直接依赖 product/crm 的 Mapper 和 Entity")
@SpringBootTest
class BusinessModelWorkflowIT {
}
