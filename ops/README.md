# Jasmine 运维与生产部署手册

本目录为 Jasmine 微服务项目的 Docker 容器化部署基础设施，支持 `linux/amd64` 与 `linux/arm64` 双架构。

---

## 一、部署架构

```
用户浏览器 (HTTP 80)
     │
     ▼
┌──────────────┐    API →    ┌──────────────┐
│  frontend    │────────────▶│   gateway    │
│  (Nginx:80)  │             │  (8080)      │
└──────────────┘             └──────┬───────┘
        │                           │ 注册/配置发现
        │                           ▼
        │                    ┌──────────────┐
        │                    │    nacos     │ (3.0.3, 鉴权开启)
        │                    │  (8848)      │
        │                    └──────────────┘
        │
        ▼
┌─────────────────────────────────────────────┐
│   IAM (jasmine_iam)    Product (jasmine_product)  │
│   Trade (jasmine_trade) CRM (jasmine_crm)        │
│   各服务独立数据库，Flyway 分库迁移                  │
└─────────────────────────────────────────────┘
        │
        ▼
┌──────────┬──────────┬──────────┐
│  MySQL   │  Redis   │ RabbitMQ │ (核心中间件)
│  (3306)  │  (6379)  │  (5672)  │
└──────────┴──────────┴──────────┘
```

---

## 二、目录结构

```
ops/
├── README.md                # 本手册
├── .env.example             # 全局环境变量模板
├── docker/                  # Docker 版（docker compose）
│   ├── dev/                 # 本地开发：仅中间件
│   │   ├── docker-compose.yml   # 开发中间件编排
│   │   └── up.sh / down.sh      # 启停脚本
│   └── prod/                # 生产：全栈
│       ├── docker-compose.yml   # 生产全栈编排
│       ├── .env                 # 生产环境变量（需从 .env.example 创建）
│       ├── nacos-application.properties  # Nacos 3.0.3 配置（鉴权开启）
│       ├── init-databases.sql   # 创建 4 个微服务独立数据库 + nacos 库
│       ├── nacos-schema.sql     # Nacos 官方表结构
│       ├── up.sh / down.sh      # 启停脚本
│       └── data/                # 持久化数据卷（自动创建）
├── podman/                  # Podman + docker compose 版
│   └── prod/                # 生产：全栈（复用 docker/prod 的 SQL 与 Nacos 配置）
│       ├── docker-compose.yml   # Podman 适配版全栈编排
│       ├── up.sh / down.sh      # 启停脚本
│       └── README.md            # Podman 部署说明
├── examples/                # 独立参考示例（不参与主流程部署）
│   └── rabbitmq/            #   仅单独启动 RabbitMQ（已被全栈编排内置取代）
│       ├── docker-compose.yml
│       └── .env.example
└── nacos-config/
    ├── import.sh            # Nacos 3.0 配置导入脚本（鉴权自适应）
    ├── jasmine-common.yml   # 公共配置
    ├── jasmine-db-common.yml
    ├── jasmine-gateway.yml
    ├── jasmine-iam.yml      # IAM 数据库 URL → jasmine_iam
    ├── jasmine-crm.yml      # CRM 数据库 URL → jasmine_crm
    ├── jasmine-trade.yml    # Trade 数据库 URL → jasmine_trade
    ├── jasmine-product.yml  # Product 数据库 URL → jasmine_product
    ├── jasmine-mq-common.yml
    ├── jasmine-redis-common.yml
    └── jasmine-observability.yml
```

---

## 三、全新部署

### 前置条件
- Docker 24+ 与 Docker Compose v2
- 磁盘空间 ≥ 5 GB

### 一键部署

```bash
git clone -b microservices https://github.com/JipZeonGit/Jasmine.git && cd Jasmine/ops/docker/prod
cp .env.example .env                      # 编辑密码和密钥
chmod +x up.sh down.sh
./up.sh                                   # 全自动四阶段部署
```

`up.sh` 自动完成：

| 阶段 | 操作 |
|------|------|
| 1 | 拉取镜像 → 启动 MySQL/Redis/RabbitMQ/Nacos → 等待 healthy |
| 2 | 创建 Nacos 命名空间 → 密码自愈（自定义密码→nacos/nacos回退→改密） → 导入 10 个 YAML 配置 |
| 3 | 启动 jasmine-schema →  Flyway 对 4 个数据库分库迁移 → 等待完成 |
| 4 | 启动所有业务服务 + 前端 → 刷新 nginx DNS |

### 验证

```bash
curl -X POST http://localhost:8080/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
# {"code":20000,"message":"success","data":{"token":"eyJ..."}}
```

浏览器访问 `http://<服务器IP>/` 进入管理后台。

---

## 四、数据库与 Flyway 架构

### 微服务分库

| 数据库 | 所属服务 | 主要表 |
|--------|---------|--------|
| `jasmine_iam` | IAM | user, role, menu, user_role, role_menu, auth_refresh_token |
| `jasmine_product` | Product | flower |
| `jasmine_trade` | Trade | sales, sales_item, inventory, inventory_alert, event_outbox |
| `jasmine_crm` | CRM | vip, appointment, site_message, event_outbox |

### Flyway 迁移脚本

```
jasmine-schema/src/main/resources/db/migration/
├── iam/       V1~V3 (baseline → auth → hardening)
├── product/   V1~V3
├── trade/     V1~V6
└── crm/       V1~V6
```

- Schema 服务（`jasmine-schema`）作为独立的一次性迁移工具运行
- 对 4 个数据库分别执行独立目录中的 Flyway 迁移
- 每个数据库维护独立的 `flyway_schema_history` 表
- 业务服务设置 `spring.flyway.enabled=false`，不参与迁移

---

## 五、Nacos 鉴权说明

生产环境 Nacos 已默认开启鉴权：

| 配置项 | 值 | 位置 |
|--------|-----|------|
| `nacos.core.auth.enabled` | `true` | `nacos-application.properties` |
| `nacos.core.auth.console.enabled` | `true` | `nacos-application.properties` |
| 默认管理员账号 | `nacos / nacos` | Nacos 内置 |
| 客户端连接 | 需提供 `NACOS_USERNAME` / `NACOS_PASSWORD` | `docker-compose.yml` service-env |

> ⚠️ 生产环境务必通过 Nacos 控制台修改默认密码，并更新 `.env` 中 `NACOS_PASSWORD`。

---

## 六、常用运维命令

### 查看容器状态
```bash
cd /opt/jasmine/ops/docker/prod
docker compose ps
```

### 查看指定服务日志
```bash
docker compose logs -f jasmine-gateway   # 网关
docker compose logs -f trade-service     # 交易服务
docker compose logs jasmine-schema       # Schema 迁移日志
```

### 重启单个服务
```bash
docker compose up -d --force-recreate <service-name>
```

### 完全重建（保留数据）
```bash
docker compose up -d --force-recreate
```

### 完全重置（清除所有数据）
```bash
docker compose down -v
# 清理数据卷
docker run --rm -v $(pwd)/data:/data alpine rm -rf /data/*
```

---

## 七、数据持久化

| 服务 | 宿主机路径 | 内容 |
|------|-----------|------|
| MySQL | `./docker/prod/data/mysql/` | 全部数据库物理文件 |
| Redis | `./docker/prod/data/redis/` | AOF 持久化文件 |
| RabbitMQ | `./docker/prod/data/rabbitmq/` | 消息和队列数据 |
| Nacos | `./docker/prod/data/nacos/logs/` | 系统运维日志 |
| 微服务 | `./docker/prod/logs/<服务名>/` | 业务运行日志 |

备份建议：定期备份 `data/mysql/` 目录（包含了所有业务数据和 Nacos 配置数据）。

---

## 八、默认账号

| 系统 | 账号 | 密码 | 用途 |
|------|------|------|------|
| 前端登录 | `admin` | `123456` | 系统管理员 |
| 前端登录 | `Jasmine` | `Jasmine` | 普通用户 |
| Nacos 控制台 | `nacos` | 由 `.env` 自定义（up.sh 自动同步） | Nacos 管理员 |

---

## 九、镜像构建

Docker 镜像由 **GitHub Actions** 自动构建（`.github/workflows/docker-publish.yml`），推送至 `ghcr.io/jipzeongit/<module>`。

支持的架构：`linux/amd64` + `linux/arm64`（通过 QEMU + buildx）。

推送到 `main` 或 `microservices` 分支即触发构建。
