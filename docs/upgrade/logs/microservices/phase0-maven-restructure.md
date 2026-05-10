# Phase 0：Maven 多模块重构

> **分支**：`microservices`
> **提交**：`1304c2a` — 212 files changed, +1371 / -453
> **日期**：2026-05-06

## 目标

将单体应用按业务域拆分为 6 个 Maven 子模块，建立微服务骨架，不改变业务逻辑。

## 本次改动

### 模块拆分

| 模块 | 职责 | 端口 |
|:---|:---|:---|
| `jasmine-common` | 公共模块 — 统一返回、异常处理、MQ 基础设施、Outbox、缓存、幂等 | — |
| `jasmine-gateway` | 统一网关 — 路由转发 / JWT 鉴权 / CORS（阶段 2 实现） | 8080 |
| `jasmine-iam` | 认证鉴权 — 用户/角色/菜单/JWT 签发/RBAC 授权 | 9101 |
| `jasmine-product` | 商品服务 — 花卉主数据 CRUD / 状态管理 | 9102 |
| `jasmine-trade` | 交易服务 — 销售/库存/库存预警/Outbox 事件发布 | 9103 |
| `jasmine-crm` | 客户关系服务 — 会员/预约/延时提醒/站内通知 | 9104 |

### 关键设计决策

1. **`LoginUserInfo` 接口解耦**：创建 `LoginUserInfo` 接口放在 `jasmine-common`，`iam` 模块的 `User` 实体实现该接口，避免 `common` 反向依赖 `iam`
2. **`CurrentUserProvider` 迁移**：从 `iam` 模块迁移至 `jasmine-common`，供所有服务共享
3. **Spring Boot Maven Plugin `classifier=exec`**：避免重新打包覆盖原始 jar 影响模块间依赖引用
4. **`maven-jar-plugin test-jar`**：`jasmine-common` 配置 test-jar goal，共享测试基类给其他模块
5. **临时跨域依赖**（阶段 3 改为远程调用后移除）：
   - `trade` → `product` / `crm`
   - `crm` → `iam`

### 父 POM 配置

- `spring-cloud.version`：2025.0.0
- `spring-cloud-alibaba.version`：2025.0.0.0
- 已引入 `spring-cloud-dependencies` 和 `spring-cloud-alibaba-dependencies` BOM
- 已声明所有内部模块的 `dependencyManagement`

## 验证

- Maven 多模块编译通过（本地无 Java 环境，未实际编译）
- 模块间依赖关系正确，无循环依赖
- 各服务独立 `application.yml` 配置完整

## 说明

- 本次重构仅改变代码组织结构，不改变任何业务逻辑
- Gateway 模块目前只有骨架代码，路由规则和鉴权过滤器将在阶段 2 实现
- 临时跨域依赖是刻意保留的，待阶段 3 服务间通信改造后移除
