# PR14 Docker / Ops / 部署整理

## 目标

把当前已经完成的后端、MySQL、Redis、RabbitMQ 和前端运行方式，整理成一套：

- 可复现
- 可联调
- 可部署
- 可回滚

的稳定运维基线。

这轮不继续做业务功能，也不继续扩 Redis / MQ 的架构能力，而是把运行环境和部署基线真正立住。

## 本轮交付物

### 1. `ops/` 目录

本轮已经统一整理为：

- `ops/.env.example`
- `ops/dev/docker-compose.yml`
- `ops/dev/up.sh`
- `ops/dev/down.sh`
- `ops/prod/docker-compose.yml`
- `ops/prod/up.sh`
- `ops/prod/down.sh`

### 2. 分环境 Compose

#### 开发环境：`ops/dev/docker-compose.yml`

特点：

- 后端镜像本地构建
- 前端镜像本地构建
- 适合在有 Docker 的本地或 NAS 上做联调验证

#### 生产环境：`ops/prod/docker-compose.yml`

特点：

- 后端镜像从 GHCR 拉取
- 前端镜像从 GHCR 拉取
- 适合部署到 NAS 或生产主机

### 3. Docker 镜像基线

当前镜像构建文件包括：

- 后端：`Dockerfile`
- 前端：`admin/Dockerfile`

后端镜像额外补了：

- `curl`

用于容器内健康检查。

## 环境变量说明

统一模板文件：

- `ops/.env.example`

首次使用时：

```bash
cp ops/.env.example ops/.env
```

需要重点修改的变量包括：

- `MYSQL_ROOT_PASSWORD`
- `MYSQL_USER`
- `MYSQL_PASSWORD`
- `REDIS_PASSWORD`
- `RABBITMQ_USERNAME`
- `RABBITMQ_PASSWORD`
- `JWT_SECRET`
- `BACKEND_IMAGE_TAG`
- `FRONTEND_IMAGE_TAG`

## Compose 服务范围

当前两套 Compose 都覆盖：

- `mysql`
- `redis`
- `rabbitmq`
- `backend`
- `frontend`

## 健康检查

本轮已经给这些服务补了基础 healthcheck：

- MySQL：`mysqladmin ping`
- Redis：`redis-cli ping`
- RabbitMQ：`rabbitmq-diagnostics ping`
- Backend：`/actuator/health`
- Frontend：`/`

这样启动后可以直接通过：

```bash
docker compose ps
```

快速确认服务是否健康。

## 日志与数据目录

本轮已经为关键服务预留持久化目录：

### 开发环境

- `ops/dev/data/mysql`
- `ops/dev/data/redis`
- `ops/dev/data/rabbitmq`
- `ops/dev/logs/backend`
- `ops/dev/logs/rabbitmq`

### 生产环境

- `ops/prod/data/mysql`
- `ops/prod/data/redis`
- `ops/prod/data/rabbitmq`
- `ops/prod/logs/backend`
- `ops/prod/logs/rabbitmq`

说明：

- MySQL / Redis / RabbitMQ 的数据目录用于持久化
- 后端日志目录通过 `LOGGING_FILE_NAME` 显式写入挂载路径
- 其他服务仍然可以通过 `docker compose logs` 查看标准输出日志

## GitHub Actions 镜像构建

当前工作流文件：

- `.github/workflows/docker-publish.yml`

本轮已经调整为：

- `push` 到 `main` 时自动构建并推送正式镜像
- `push` 到 `next` 时自动构建并推送 `next` 通道镜像
- 支持手动 `workflow_dispatch`

当前标签策略：

- `main` 分支：
  - `latest`
  - `sha-<short_sha>`
- 其他分支（例如 `next`）：
  - `<branch>-latest`
  - `<branch>-<short_sha>`

例如 `next` 分支会产出：

- `ghcr.io/<owner>/jasmine-backend:next-latest`
- `ghcr.io/<owner>/jasmine-frontend:next-latest`

## NAS 部署建议

如果使用 NAS 做部署和联调，推荐流程如下：

1. GitHub Actions 构建并推送镜像到 GHCR
2. 在 NAS 上拉取仓库或同步 `ops/` 目录
3. 复制环境变量模板：

```bash
cp ops/.env.example ops/.env
```

4. 修改：

- 密码
- 端口
- JWT 密钥
- 镜像标签

5. 启动生产环境：

```bash
chmod +x ops/prod/up.sh ops/prod/down.sh
./ops/prod/up.sh
```

当前 NAS 实际部署与联调记录见：

- `docs/upgrade/pr14-nas-deploy-validation.md`

## 管理台与联调入口

部署后建议重点检查：

- 前端：`http://<host>:${FRONTEND_PORT}`
- 后端健康检查：`http://<host>:${BACKEND_PORT}/actuator/health`
- Swagger：`http://<host>:${BACKEND_PORT}/swagger-ui/index.html`
- RabbitMQ 管理台：`http://<host>:${RABBITMQ_MANAGEMENT_PORT}`

## 初始化说明

当前初始化依赖以下机制：

- MySQL 容器通过环境变量自动创建数据库与应用用户
- 后端启动后由 Flyway 自动执行数据库迁移
- RabbitMQ 容器通过环境变量初始化默认账号与虚拟主机

因此当前不再依赖旧的根目录单体 `docker-compose.yml` 直接导入数据库脚本。

首次启动建议顺序：

1. 准备 `ops/.env`
2. 启动 Compose
3. 等待 MySQL / Redis / RabbitMQ 健康
4. 等待后端 `actuator/health` 变为 `UP`
5. 再访问前端和 Swagger

## 回滚说明

### 应用镜像回滚

如果新版本镜像启动异常，可直接把 `ops/.env` 中的：

- `BACKEND_IMAGE_TAG`
- `FRONTEND_IMAGE_TAG`

改回上一个稳定标签，再执行：

```bash
./ops/prod/up.sh
```

### 服务回滚边界

- 前端和后端镜像回滚是轻量操作
- MySQL / Redis / RabbitMQ 数据目录默认保留
- 如果数据库迁移已经执行，需要确认当前版本是否兼容旧镜像；本轮默认不提供自动数据库降级脚本

### 不建议的回滚方式

- 不要直接删除持久化数据目录再重启，除非明确接受数据清空
- 不要把数据库迁移回滚和应用镜像回滚混为一件事

## 当前结论

`PR14` 这一轮的目标不是做业务，而是把运行环境立住。

做完后应达到：

- 本地 / NAS / 服务器都能基于统一 `ops/` 目录启动环境
- GitHub Actions 能持续构建和推送镜像
- 运维入口、健康检查、日志位置、启动方式和回滚方式都清楚

这就是后面继续做 `PR15` 前端迁移之前，最需要补齐的部署基线。
