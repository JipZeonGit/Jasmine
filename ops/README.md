# Jasmine 运维与生产部署手册 (ops/README.md)

本目录为 Jasmine 微服务项目统一的 **Docker 容器化与运维基础设施基线**。项目已全面适配 **`linux/amd64` (x86_64)** 与 **`linux/arm64` (aarch64 / Apple Silicon)** 双主流硬件架构，实现了一键式、零差异的多架构混合部署。

---

## 一、 部署架构概览

```text
                  【 外部用户浏览器 (HTTP 80) 】
                                │
                                ▼
                   ┌────────────────────────┐
                   │    jasmine-frontend    │ (Nginx 静态网页容器)
                   └───────────┬────────────┘
                               │
                API 请求跨域    │ (经由外部网络)
                ───────────────┼──────────────┐
                               │              │
                               ▼              ▼
                   ┌────────────────────────┐┌────────────────────────┐
                   │    jasmine-gateway     ││     nacos-server       │ (Nacos 配置/注册中心)
                   │      (API 网关 8080)     ││      (端口 8848)       │
                   └───────────┬────────────┘└───────────┬────────────┘
                               │                         │ 注册发现与拉取配置
                               ├─────────────────────────┤
                               ▼
     ┌──────────────────┬──────────────┬──────────────────┐ (内网防护校验网关 Token)
     ▼                  ▼              ▼                  ▼
┌──────────┐      ┌──────────┐   ┌──────────┐       ┌──────────┐
│   IAM    │      │ Product  │   │  Trade   │       │   CRM    │ (微服务集群)
│ (9101)   │      │  (9102)  │   │  (9103)  │       │  (9104)  │
└────┬─────┘      └────┬─────┘   └────┬─────┘       └────┬─────┘
     │                 │              │                  │
     ├─────────────────┼──────────────┼──────────────────┤ (内部高可靠性数据存取)
     ▼                 ▼              ▼                  ▼
┌──────────┐      ┌──────────┐   ┌──────────┐       ┌──────────┐
│  MySQL   │      │  Redis   │   │ RabbitMQ │       │  Nacos   │ (核心中间件群)
│ (3306)   │      │ (6379)   │   │ (5672)   │       │  (3306)  │
└──────────┘      └──────────┘   └──────────┘       └──────────┘
```

---

## 二、 目录结构说明

本目录包含开发环境和生产环境两套隔离的部署体系：

```text
ops/
├── README.md               # 本运维手册
├── .env.example            # 全局通用环境变量模板（供本地构建参考）
├── dev/                    # 【本地开发环境】 (仅部署 MySQL/Redis/MQ/Nacos 四个中间件)
│   ├── docker-compose.yml
│   ├── init-databases.sql
│   ├── up.sh
│   └── down.sh
├── prod/                   # 【生产环境】 (全栈一键拉起：中间件 + 5 微服务 + 网关 + 前端)
│   ├── docker-compose.yml  # 生产容器编排服务定义
│   ├── .env.example        # 生产专属环境变量模板（开箱即用，标准端口配置）
│   ├── init-databases.sql  # 自动创建 5 个独立微服务数据库
│   ├── up.sh               # 生产一键启动脚本
│   └── down.sh             # 生产一键停止脚本
└── nacos-config/           # 【Nacos 配置导入包】
    ├── import.sh           # Nacos 配置自动解包并安全导入脚本
    └── nacos_config_export.zip # 默认微服务配置包
```

---

## 三、 生产环境部署操作指南

本指南适用于将 Jasmine 部署于 **CentOS/Ubuntu/AWS Graviton/阿里云 Arm 实例/本地 macOS** 等物理或虚拟服务器中。

### 步骤 1：拷贝运维文件至服务器
在服务器中创建主工作目录（如 `/opt/jasmine`），将项目的 `ops/` 目录拷贝至该路径下。保证结构如下：
```bash
/opt/jasmine/
└── ops/
    ├── dev/
    ├── prod/
    ├── nacos-config/
    └── .env.example
```

### 步骤 2：创建并编辑 `.env` 生产环境配置文件
1. 进入 `/opt/jasmine/ops` 目录，将生产专属模板复制为工作环境文件：
   ```bash
   cd /opt/jasmine/ops
   cp prod/.env.example .env
   ```
2. 编辑 `.env` 文件，修改以下核心安全字段：
   - **安全随机密钥**：
     - **`JWT_SECRET`**：签名密钥。使用头部注释中的 `openssl rand -base64 64` 命令生成，避免越权安全隐患。
     - **`NACOS_AUTH_TOKEN`**：Nacos 2.x 安全 Token。使用 `openssl rand -base64 32` 命令生成并替换。
   - **组件安全密码**：
     - 将 `MYSQL_ROOT_PASSWORD`、`MYSQL_PASSWORD`、`REDIS_PASSWORD`、`RABBITMQ_PASSWORD` 更改为您独有的强随机密码。
   - **跨域与域名绑定**：
     - `CORS_ALLOWED_ORIGINS` 默认配置为 `http://localhost,http://127.0.0.1` 满足开箱即用。若绑定了公网域名，请务必追加，如 `,https://jasmine.yourdomain.com`。

### 步骤 3：初次启动中间件并自动初始化数据库
1. 给启动脚本赋予执行权限，并执行：
   ```bash
   chmod +x prod/up.sh prod/down.sh
   ./prod/up.sh
   ```
2. **多库初始化机制**：
   - 编排已配置挂载了宿主机物理路径 `ops/prod/data/mysql` 保证数据持久化。
   - 首次拉起 MySQL 容器时，会自动加载 `init-databases.sql` 脚本，在库中创建 `nacos`、`jasmine_iam`、`jasmine_product`、`jasmine_trade`、`jasmine_crm` 5 个独立的微服务 Schema，并进行精准权限分配。

### 步骤 4：导入 Nacos 微服务配置
在微服务（Gateway/IAM 等）拉起之前，必须先把持久化的应用配置文件刷入 Nacos 配置中心中：
1. 进入配置导入目录：
   ```bash
   cd /opt/jasmine/ops/nacos-config
   ```
2. 执行一键导入脚本：
   ```bash
   # 脚本会自动根据 ../.env 中配置的账号密码、Nacos 鉴权 Token 进行合法登录并无缝导入
   bash import.sh 127.0.0.1:8848 prod
   ```

### 步骤 5：启动全栈应用与 Flyway 自动表结构迁移
1. 返回并再次运行启动脚本以拉起全部应用：
   ```bash
   cd /opt/jasmine/ops
   ./prod/up.sh
   ```
2. **Flyway 表结构自动迁移**：
   - 编排中的 `jasmine-schema` 服务会作为一次性迁移工具（Migration Tool）最先启动。
   - 它会连接 MySQL 自动对 4 个独立的业务 Schema 跑 Flyway 迁移，建立全部表结构并灌入初始测试数据。
   - 迁移成功后 `jasmine-schema` 会以退出码 `0` 自动退出释放系统资源。随后各核心微服务（Gateway/IAM/Product 等）有序注册上线，前端 Nginx 正常对外工作。

---

## 四、 常见运维诊断命令

### 1. 查看容器运行健康状态
```bash
docker compose -f prod/docker-compose.yml ps
```
正常运行时，5 个业务服务、Gateway、Frontend 和 4 个中间件的状态应均显示为 `Up (healthy)`。

### 2. 实时滚动查看业务运行日志
例如查看网关（Gateway）与交易服务（Trade）的日志：
```bash
docker compose -f prod/docker-compose.yml logs -f jasmine-gateway
docker compose -f prod/docker-compose.yml logs -f trade-service
```

### 3. 在 ARM64 架构下验证 JVM 运行状态
我们采用的后端容器完全基于 **Java 21 + 原生 ARM64 JRE** 运行：
1. 监控 ZGC 垃圾回收器状态：
   ```bash
   docker compose -f prod/docker-compose.yml logs -f crm-service | grep -i "gc"
   ```
2. 校验物理内存限额（512M）：
   ```bash
   # 查看容器占用物理内存详情与 JVM 缩放是否限制在 512MB 内
   docker stats jasmine-prod-iam
   ```

---

## 五、 数据卷持久化与数据备份指南

生产环境已经做好了全套数据卷持久化，所有核心持久化数据直接落地到宿主机的物理磁盘上：

| 服务 | 宿主机物理挂载路径 | 容器内挂载路径 | 作用说明 |
| :--- | :--- | :--- | :--- |
| **MySQL** | `./prod/data/mysql` | `/var/lib/mysql` | 存储全部 5 个独立库的物理表与索引文件 |
| **Redis** | `./prod/data/redis` | `/data` | 存储 AOF (Append Only File) 持久化文件 |
| **RabbitMQ**| `./prod/data/rabbitmq`| `/var/lib/rabbitmq` | 存储持久化消息、交换机与高可用队列数据 |
| **Nacos** | `./prod/data/nacos/logs`| `/home/nacos/logs` | 存储配置中心的系统运维日志 |
| **微服务日志**| `./prod/logs/<service>`| `/app/logs` | 挂载出运行日志，方便通过 ELK/Filebeat 收集 |

### 备份建议
- **数据库冷备**：建议定期在宿主机备份整个 `/opt/jasmine/ops/prod/data/mysql` 物理目录，或者在 MySQL 容器内运行 `mysqldump` 定时输出 SQL 归档。
- **配置冷备**：Nacos 的配置文件会直接保存在 MySQL 的 `nacos` 库中，备份了 MySQL 即可完成对 Nacos 全部注册配置信息的完整备份。
