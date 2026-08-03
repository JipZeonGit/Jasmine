# Jasmine —— Podman + Docker Compose 部署

本目录提供一份 **适配 Podman** 的 Jasmine 生产全栈编排，供 Fedora 等本机使用
**Podman 作为容器运行时、同时用 `docker compose` CLI（而非 `podman compose`）** 的场景。

> 与 `ops/docker/prod/docker-compose.yml` 功能等价，但针对 Podman 做了适配，
> 且不会改动原有 Docker 相关配置（`ops/docker/dev`、`ops/docker/prod` 保持原样）。

---

## 一、目录结构

```
ops/
├── docker/
│   ├── dev/        # Docker 版：本地开发中间件（docker compose）
│   └── prod/       # Docker 版：生产全栈（docker compose）
└── podman/
    └── prod/       # Podman 版：生产全栈（docker compose + Podman 后端）
        ├── docker-compose.yml    # Podman 适配版全栈编排（中间件 + Schema + 服务 + 前端）
        ├── .env.example          # 环境变量模板（基于 prod 版，前端端口改为 8081）
        ├── up.sh                 # 一键部署（四阶段：中间件 → Nacos 配置 → Schema → 服务）
        ├── down.sh               # 一键停止
        └── data/ logs/           # 运行时自动生成（持久化数据 / 日志）
```

> SQL 初始化脚本与 Nacos 配置直接复用 `ops/docker/prod/` 下的文件（单一来源）：
> - `../../docker/prod/init-databases.sql`
> - `../../docker/prod/nacos-schema.sql`
> - `../../docker/prod/nacos-application.properties`

---

## 二、与 Docker 版的关键差异

| 项目 | Docker 版 (ops/docker/prod) | Podman 版 (ops/podman/prod) | 原因 |
|------|---------------------|------------------------|------|
| 前端端口 | `80` | `8081`（默认） | Podman rootless 无法绑定 `<1024` 端口 |
| 资源限制 | `deploy.resources` + `mem_limit` | 仅 `mem_limit` | 非 swarm 下 `deploy` 被忽略，Podman 后端不识别 |
| 镜像拉取 | `pull_policy: always` | `pull_policy: missing` | 更贴合 Podman 语义，避免每次启动强制联网 |
| 容器名 | `jasmine-prod-*` | `jasmine-podman-*` | 避免与 Docker 版同名冲突 |
| 项目名 | `jasmine-prod` | `jasmine-podman` | 同上 |

---

## 三、一键部署

### 前置条件

- Fedora 等已安装 Podman
- **Docker Compose CLI 能连通 Podman**（见下方第 4 节）
- 已安装 `podman` CLI（脚本内部用 `podman` 查询/执行容器）
- 磁盘空间 ≥ 5 GB

### 步骤

```bash
cd ops/podman/prod
cp .env.example .env          # 编辑密码、密钥、端口
chmod +x up.sh down.sh
./up.sh
```

`up.sh` 自动完成：

| 阶段 | 操作 |
|------|------|
| 1 | 拉取镜像 → 启动 MySQL/Redis/RabbitMQ/Nacos → 等待 healthy |
| 2 | 创建 Nacos 命名空间 → 密码自愈 → 导入 10 个 YAML 配置 |
| 3 | 启动 jasmine-schema → Flyway 分库迁移 → 等待完成 |
| 4 | 启动所有业务服务 + 前端 → 刷新 nginx DNS |

### 验证

```bash
curl -X POST http://localhost:8080/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
# {"code":20000,"message":"success","data":{"token":"eyJ..."}}
```

浏览器访问 `http://localhost:8081/` 进入管理后台。

### 停止

```bash
./down.sh
```

---

## 四、让 `docker compose` 连通 Podman（重要）

`docker compose`（v2 Go 版本）本身不直接驱动 Podman，需要在 Fedora 上配置
**Docker 兼容 socket**，让 docker compose CLI 通过 `DOCKER_HOST` 指向 Podman。

推荐方式（Podman ≥ 4.4，systemd 用户级 socket）：

```bash
# 1. 启用并启动 Podman 的 Docker 兼容 socket（用户级）
systemctl --user enable --now podman.socket

# 2. 让当前 shell 的 docker compose 走 Podman
export DOCKER_HOST=unix://$XDG_RUNTIME_DIR/podman/podman.sock

# 3. 验证
docker compose version          # 应能识别
docker ps                       # 应能列出 Podman 容器（相当于 podman ps）
```

若希望全局生效，可将 `export DOCKER_HOST=...` 写入 `~/.bashrc`。

> 备选方案：安装 `podman-docker` 可提供 `/usr/bin/docker` 兼容层，
> 但 `docker compose` 子命令仍需通过上述 socket 方式才能驱动 Podman。
> 若你的 docker compose 已能正常工作，请忽略本节。

---

## 五、常见问题

### 1. 前端端口 80 绑定失败（Permission denied）
Podman rootless 无法绑定 `<1024` 端口。默认已用 `8081`。
若坚持用 80，请以 root 运行 Podman（rootful）：
```bash
sudo systemctl enable --now podman.socket   # rootful socket
sudo env DOCKER_HOST=unix:///run/podman/podman.sock ./up.sh
```

### 2. 镜像拉取失败（ghcr.io 鉴权）
从 `ghcr.io` 拉取公共镜像无需登录；若镜像为私有，请先：
```bash
podman login ghcr.io -u <GitHub用户名>
```

### 3. `pull_policy: missing` 不会更新本地已存在的旧镜像
如需强制拉取最新镜像，手动执行：
```bash
docker compose --env-file .env pull
docker compose --env-file .env up -d --force-recreate
```

### 4. Nacos 控制台
浏览器访问 `http://localhost:8848/nacos/`，账号 `nacos`，密码为 `.env` 中
`NACOS_PASSWORD`（up.sh 会自动同步）。

---

## 六、常用运维命令

```bash
cd ops/podman/prod
docker compose --env-file .env ps                      # 查看状态
docker compose --env-file .env logs -f trade-service   # 跟踪日志
podman logs jasmine-podman-schema --tail=20            # Schema 迁移日志
docker compose --env-file .env up -d --force-recreate  # 完全重建（保留数据）
docker compose --env-file .env down -v                 # 停止并清空数据卷
```
