# 项目阶段状态

当前主分支 `microservices`，已完成从单体到微服务架构的完整迁移。

---

## 当前架构

```
Jasmine 微服务（Spring Cloud 2025.0.0 + Nacos 3.0.3）

  frontend (Vue 3 + Element Plus, Nginx:80)
      │
  gateway (Spring Cloud Gateway, :8080)
      │
  ┌─────┬─────────┬─────────┬─────────┐
  │ IAM │ Product │  Trade  │   CRM   │
  │9101 │  9102   │  9103   │  9104   │
  └──┬──┴────┬────┴────┬────┴────┬────┘
     │       │         │         │
  jasmine  jasmine  jasmine  jasmine
   _iam   _product  _trade    _crm      (独立数据库)
     │       │         │         │
  ┌──┴───────┴─────────┴─────────┴──────┐
  │  MySQL 8.4  │  Redis 7.2  │  RabbitMQ 4.2  │
  └──────────────────────────────────────┘
```

---

## 核心功能

| 模块 | 功能 | 状态 |
|------|------|------|
| 用户认证 | JWT 登录/刷新/登出、RBAC 权限 | ✅ |
| 花卉管理 | 主数据 CRUD、库存快照 | ✅ |
| 库存管理 | 采购入库/销售出库、库存预警 | ✅ |
| 销售管理 | 销售单创建/查询、今日汇总 | ✅ |
| 会员管理 | 会员注册/查询 | ✅ |
| 预约管理 | 预约创建、延时提醒（站内信红点） | ✅ |
| 站内信 | 消息中心、未读计数、已读标记 | ✅ |
| 消息可靠性 | Outbox 本地消息表 + 死信队列 + 重试 | ✅ |

---

## 生产部署

| 项 | 状态 |
|-----|------|
| Docker Compose 全栈编排 | ✅ |
| `./up.sh` 一键部署（4 阶段自动化） | ✅ |
| Nacos 3.0.3 鉴权开启（密码自愈） | ✅ |
| Flyway 分库迁移（jasmine-schema 集中管理） | ✅ |
| GitHub Actions 多架构镜像（amd64/arm64） | ✅ |
| 优雅关闭 / 网关限流 / 前端幂等键 | ✅ |

---

## 微服务迁移历程

详细记录见 [docs/upgrade/logs/microservices/](docs/upgrade/logs/microservices/)：

| 阶段 | 内容 | 文档 |
|------|------|------|
| Phase0 | Maven 多模块拆分 | [phase0](docs/upgrade/logs/microservices/phase0-maven-restructure.md) |
| Phase1 | Nacos 服务注册与配置中心接入 | [phase1](docs/upgrade/logs/microservices/phase1-nacos-integration.md) |
| Phase2 | Gateway 路由与全局 JWT 鉴权 | [phase2](docs/upgrade/logs/microservices/phase2-gateway-routing.md) |
| Phase3 | 服务间 RestClient 远程调用 | [phase3](docs/upgrade/logs/microservices/phase3-service-communication.md) |
| Phase4 | 数据库按服务拆分 | [phase4](docs/upgrade/logs/microservices/phase4-database-split.md) |
| Phase5 | 前端适配微服务架构 | [phase5](docs/upgrade/logs/microservices/phase5-frontend-adaptation.md) |
| Phase6 | 架构加固（优雅关闭/限流/幂等） | [phase6](docs/upgrade/logs/microservices/phase3-phase6-remote-decoupling-and-hardening.md) |

---

## 后续计划

- 预约超时自动取消
- 销售数据仓库与 CQRS 读模型
- 外部通知渠道（微信/短信/邮件）
- 复杂单据状态机
- 全链路可观测性（分布式追踪/指标/告警）

---

## 历史存档

单体应用时期的 PR0~PR20 升级记录归档于 [docs/upgrade/logs/monolith/](docs/upgrade/logs/monolith/)。
