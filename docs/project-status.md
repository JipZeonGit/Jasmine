# 项目阶段状态

这份文档用于记录 `Jasmine` 当前从 `PR0` 到最新阶段的升级进度，避免把大量阶段性说明直接堆进 `README.md`。

## `PR0` 冻结旧系统

- `main` 保持 legacy 可运行
- `legacy-v1` 作为旧系统保留标签
- `next` 作为升级主线

状态：

- 基本完成
- 功能清单 / 接口清单 / 升级边界文档仍可继续补全

## `PR1` 升级基线

- 分环境配置
- 敏感配置环境变量化
- GitHub Actions 基础 build / test / package 思路

状态：

- 部分完成
- 剩余内容已在 `PR5`、`PR6` 中继续收口

## `PR2` 后端底座升级

- JDK 21
- Spring Boot 3.5.x
- `javax -> jakarta`
- Boot 3 对应依赖升级

状态：

- 基本完成

## `PR3` 认证鉴权第一阶段

- `SecurityFilterChain`
- JWT Filter
- Bearer 鉴权主链
- 单体 RBAC

状态：

- 主链路完成
- 深层欠账已在后续阶段继续收口

## `PR4` 接口规范第一阶段

- 用户主链路 DTO / VO
- `@Valid`
- 全局异常处理
- 部分统一返回结构

状态：

- 主链路完成
- 全量规范化已后移到后续阶段

## `PR5` 基础设施收口

- Flyway baseline
- 基础健康检查
- JWT 密钥环境变量化
- 升级文档与运行文档补齐

状态：

- 已完成

## `PR6` 测试基线现代化

- Testcontainers MySQL
- Testcontainers Redis
- 集成测试基线现代化

状态：

- 已完成

## `PR7` 认证鉴权第二阶段

- refresh token / logout / 失效策略
- token 生命周期设计
- 认证文档补齐

状态：

- 已完成

## `PR8` 接口规范第二阶段

- DTO / VO 边界继续收口
- 错误码统一
- 接口返回继续规范化

状态：

- 已完成

## `PR9` 日志与指标基线

- 统一日志
- `traceId` / `requestId`
- Micrometer
- Prometheus endpoint

状态：

- 已完成

## `PR10` 数据库正式升级

- MySQL 8.4 基线
- 字符集 / 排序规则 / SQL 兼容性收口
- 数据库迁移验证

状态：

- 已完成

## `PR11` 核心业务模型重建

- 花卉 / 库存 / 销售 / 会员 / 预约主模型重建
- 业务主链重新梳理

状态：

- 已完成

## `PR12` 模块化单体重组

- 按业务域重组后端代码
- 收口模块边界和目录结构

状态：

- 已完成

## `PR12.5` Redis 收口

- Redis 缓存边界明确
- 菜单 / 角色 / 花卉等主数据缓存收口

状态：

- 已完成

## `PR13` RabbitMQ 接入

- RabbitMQ 拓扑接入
- 预约 / 审计 / 销售 / 库存事件链路
- 重试、死信、契约文档、行为测试

状态：

- 已完成

## `PR13.5` Redis 与 MQ 稳定性收口

- 库存原子更新
- 请求级幂等
- 缓存空值安全与热点保护
- TTL 抖动
- MQ 发布确认可观测性

状态：

- 已完成

## `PR14` Docker / Ops / 部署整理

- `ops/` 目录
- 分环境 Compose
- `.env.example`
- GitHub Actions 镜像构建
- 部署 / 回滚文档

状态：

- 进行中

## `PR15` 新前端全量迁移及美化

- Vite + Vue 3 + Element Plus 全量替换旧前端
- 系统级及所有业务页面迁移合并
- 深度汉化修复（Element Plus 日期时间组件等）
- 主视觉重构（生机活力阳光主题 + 毛玻璃特效）
- 浅色/暗黑模式（Dark Mode）一键切换支持
- Docker / CI 构建链并入新前端主干

状态：

- 已完成

## 后续主线

- 后端继续承接更重的高并发与一致性能力
- 持续完善全系统的端到端监控与防护
