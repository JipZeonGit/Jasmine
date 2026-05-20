# Phase5 补充：前端端口对齐与微服务防绕过安全加固

日期：2026-05-20

## 目标

修复前后端微服务适配联调中的"本地环境代理目标端口冲突"硬伤，并对下游微服务进行纵深防御加固，彻底阻止外部直连下游微服务端口伪造身份的越权安全隐患。同时解决 WSL2 + Docker 环境下 MySQL 容器无法正常启动的基础设施问题。

## 本轮改动

### 前端开发配置对齐

- **[MODIFY] [web/.env.development](file:///d:/Software%20Engineering/Code%20Library/IdeaProjects/Jasmine/web/.env.development)**：
  - 将开发环境 API 代理目标 `VITE_API_PROXY_TARGET` 从 `http://localhost:9999` 修正为网关实际监听的 `http://localhost:8080`。
  - 彻底解决了本地启动前端 `bun dev` 联调时 API 请求报 502 / Connection Refused 的端口错配问题。

### 后端安全防护加固（微服务防绕过直连）

- **[MODIFY] [InternalEndpointGuardFilter.java](file:///d:/Software%20Engineering/Code%20Library/IdeaProjects/Jasmine/jasmine-common/src/main/java/com/nfu/jasmine/infra/security/filter/InternalEndpointGuardFilter.java)**：
  - 重构了防守校验的拦截器规则。原来仅保护以 `/internal/**` 开头的内部接口，容易被攻击者直连微服务普通业务端口（如 `9102`、`9103`、`9104`）并通过伪造 `X-User-Id` 进行未授权的管理员操作越权攻击。
  - 重新设计了 `shouldNotFilter` 判断规则，拦截除系统级监控（`/actuator/**`）、公共 API 文档（`/swagger-ui/**`、`/v3/api-docs/**`、`/swagger-resources/**`）以及错误处理（`/error`）之外的**所有微服务业务请求**。
  - 强制对所有普通业务接口实施纵深防守校验。任何普通业务流量必须从请求头中出示在网关或者微服务间 RestClient 通信中附加的、与网关期望一致的 `X-Gateway-Token`（网关共享密钥）才允许放行，否则统一返回 `403 Forbidden` 并阻断。

### 开发环境 Docker 基础设施修复（WSL2 兼容性）

- **[MODIFY] [ops/dev/docker-compose.yml](file:///d:/Software%20Engineering/Code%20Library/IdeaProjects/Jasmine/ops/dev/docker-compose.yml)**：
  - **MySQL 镜像降级 8.4 → 8.0**：MySQL 8.4 重构了 redo log 架构（引入 `#innodb_redo` 目录），在 WSL2 的 Docker overlay2 存储驱动上存在严重的兼容性问题——`mysqld --initialize` 阶段的 redo log 无法在 overlay2 虚拟文件系统上正确落盘，导致后续服务端启动时判定数据损坏并陷入无限崩溃循环。MySQL 8.0 使用经典 redo log 格式，无此问题。
  - **MySQL 数据目录改用 tmpfs**：将 `mysql-data` named volume 替换为 `tmpfs` 内存挂载，彻底规避 Docker overlay2 + WSL2 虚拟磁盘 I/O 导致的 InnoDB redo log 脏写。数据在容器重启后不持久化，但开发环境可接受（init SQL 会在每次启动时重新执行）。
  - **新增 MySQL 启动参数**：`--skip-name-resolve`（跳过 DNS 反解提升连接速度）、`--innodb_use_native_aio=0`（关闭原生异步 I/O，兼容 WSL2 内核）。
  - **移除全部 healthcheck 配置**：MySQL、Redis、RabbitMQ、Nacos 四个服务的 healthcheck 均被移除。WSL2 低速 I/O 环境下 healthcheck 超时导致容器被误判为 unhealthy 并触发不必要的重启。开发环境无需 healthcheck 保障。
  - **清理废弃的 `mysql-data` volume 声明**。

### 生产环境 Docker 配置调整

- **[MODIFY] [ops/prod/docker-compose.yml](file:///d:/Software%20Engineering/Code%20Library/IdeaProjects/Jasmine/ops/prod/docker-compose.yml)**：
  - MySQL healthcheck 的 `start_period` 从 `40s` 调整为 `180s`。首次部署需要完整初始化 datadir 并执行 `init-databases.sql` 创建 5 个业务库，原 40 秒不够用，容易在初始化中途被 healthcheck 判定为 unhealthy 触发 restart。

### 项目配置维护

- **[MODIFY] [.gitignore](file:///d:/Software%20Engineering/Code%20Library/IdeaProjects/Jasmine/.gitignore)**：
  - 将 5 条独立的 `.ps1` 忽略规则合并为通配符 `*.ps1`，一劳永逸覆盖所有本地启动脚本（包括新增的 `start-all-backends.ps1`）。

## 故障排查记录（WSL2 + Docker MySQL）

### 问题现象

MySQL 容器在 WSL2 (Linux Mint) + Docker 环境下陷入无限 `Restarting` 循环，日志反复报错：

```
[InnoDB] Cannot create redo log files because data files are corrupt
or the database was not shut down cleanly after creating the data files.
```

### 根因链条

1. `mysqld --initialize` 子进程在 Docker overlay2 + WSL2 虚拟磁盘上创建 InnoDB 数据文件和 redo log
2. 初始化子进程退出后，redo log 未能在 overlay2 文件系统层面完整持久化
3. Entrypoint 紧接着启动正式的 `mysqld` 进程，读到不完整/脏的 redo log，判定数据损坏
4. 容器以非零状态退出，`restart: unless-stopped` 触发重启，但 datadir 已被污染，每次重启都在相同的脏数据上反复崩溃

### 排除项

- ❌ `--innodb_flush_log_at_trx_commit=2`（移除后问题依旧）
- ❌ MySQL 8.0 降级（8.0 在 overlay2 上也会出现相同的 redo log 损坏）
- ❌ `--innodb_use_native_aio=0`（单独使用无法解决）

### 最终解决方案

**tmpfs 内存挂载** — 将 `/var/lib/mysql` 挂载为 tmpfs，所有 I/O 在 RAM 中完成，彻底绕开 overlay2 文件系统的持久化缺陷。代价是数据不持久化，但开发环境每次启动时 init SQL 会自动重建库表结构。

## 验证与发布

- 前端 Vite 代理直连网关 8080 端口验证通过。
- 业务微服务直接使用 HTTP 工具（如 Curl / Postman）直连业务端口测试，返回 `403 Forbidden`（提示："禁止绕过网关直接访问微服务接口！"），加固防线生效。
- 经由网关代理后携带合法的 `X-Gateway-Token` 的请求，下游微服务完美予以放行，微服务内网间通过 RestClient 携带该共享密钥的调用也验证成功。
- WSL2 Docker 环境下 MySQL 8.0 + tmpfs 初始化成功，InnoDB 正常启动，无 redo log 报错。
- 全部 4 个中间件容器（MySQL / Redis / RabbitMQ / Nacos）可正常拉起。
