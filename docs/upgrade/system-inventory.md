# 当前系统清单

## 分支与升级主线

- `main`：保留 legacy 可运行版本，用于回溯和对照
- `legacy-v1`：旧系统冻结标签
- `next`：升级主线

当前建议是不再回头在 `main` 上继续做业务演进，legacy 中发现的业务模型问题会在后续单独的业务模型重构 PR 中处理。

## 现有功能模块

### 系统管理

- 用户管理
- 角色管理
- 菜单管理
- 登录 / 当前用户信息 / 修改密码

### 门店业务

- 花卉管理
- 库存管理
- 销售管理
- 会员管理
- 预约管理

## 当前后端接口清单

### 用户与认证

- `GET /user/all`
- `POST /user/login`
- `GET /user/info`
- `POST /user/logout`
- `GET /user/list`
- `POST /user`
- `PUT /user`
- `GET /user/{id}`
- `DELETE /user/{id}`
- `PUT /user/changePassword`

### 角色与菜单

- `GET /role/list`
- `POST /role`
- `PUT /role`
- `GET /role/{id}`
- `DELETE /role/{id}`
- `GET /role/all`
- `GET /menu`

### 花卉管理

- `GET /flower/all`
- `POST /flower`
- `PUT /flower`
- `GET /flower/{id}`
- `DELETE /flower/{id}`
- `GET /flower/list`

### 库存管理

- `GET /inventory/all`
- `POST /inventory`
- `PUT /inventory`
- `GET /inventory/{id}`
- `DELETE /inventory/{id}`
- `GET /inventory/list`

### 销售管理

- `GET /sales/all`
- `POST /sales`
- `PUT /sales`
- `GET /sales/{id}`
- `DELETE /sales/{id}`
- `GET /sales/list`

### 会员管理

- `GET /vip/all`
- `POST /vip`
- `PUT /vip`
- `GET /vip/{id}`
- `DELETE /vip/{id}`
- `GET /vip/list`

### 预约管理

- `GET /appointment/all`
- `POST /appointment`
- `PUT /appointment`
- `GET /appointment/{id}`
- `DELETE /appointment/{id}`
- `GET /appointment/list`

### 观测接口

- `GET /actuator/health`
- `GET /actuator/info`

## 当前关键配置入口

### 基础运行配置

- `src/main/resources/application.yml`
- `src/main/resources/application-dev.yml`
- `src/main/resources/application-prod.yml`
- `src/test/resources/application-integration.yml`

### 推荐环境变量

- `MYSQL_HOST`
- `MYSQL_PORT`
- `MYSQL_USER`
- `MYSQL_PASSWORD`
- `REDIS_HOST`
- `REDIS_PORT`
- `REDIS_PASSWORD`
- `JWT_SECRET`
- `JWT_EXPIRE_MILLIS`
- `CORS_ALLOWED_ORIGINS`

### 当前开发环境默认约定

- `application-dev.yml` 默认连接 `MYSQL_HOST=192.168.31.26`
- 为了兼容 5.7 与 8.4 并存，默认 `MYSQL_PORT=13306`
- 如果后续只保留 8.4 并恢复宿主机 `3306`，可通过环境变量把 `MYSQL_PORT` 改回 `3306`

### 当前测试基线约定

- 轻量测试：本机运行 `./mvnw test`
- 完整集成测试：GitHub Actions 在 `verify` 阶段使用 Testcontainers 拉起 MySQL 8.4 / Redis 7.x
- 对应配置入口：`src/test/resources/application-integration.yml`

## 当前已知待后移问题

### 认证鉴权欠账

- JWT 仍然存放完整 `User`，还没有精简成必要 claims
- `refresh / logout` 仍缺完整失效策略

### 接口层欠账

- 只有用户主链路完成了较完整的 DTO / VO 规范化
- 其他业务控制器仍大量直接收发 entity
- 列表接口仍存在较多 `Map<String, Object>` 返回

### 业务模型欠账

legacy 版本中最明显的现实业务问题包括：

- 库存仍是“手工录入结果”，而不是“录入业务动作后系统自动推导结果”
- 销售仍是金额日志，不是真正的销售单与销售明细模型
- 预约与会员之间仍是松散拷贝关系，不是稳定实体关系
- 数据库约束偏弱，部分关系表历史上出现过孤儿数据

这些问题已经确认需要在后续单独的大项重构 PR 中处理，不再回头在 `main / legacy` 上修补。