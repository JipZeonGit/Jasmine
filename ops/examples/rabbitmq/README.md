# 单独启动 RabbitMQ（参考示例）

> ⚠️ **仅作参考**。当前生产/开发全栈编排（`ops/docker/prod`、`ops/docker/dev`、
> `ops/podman/prod`）均已内置 `rabbitmq` 服务，**无需**再用本示例单独启动。
>
> 本示例源自早期过渡方案，保留以作独立启动 / 联调 RabbitMQ 的参考。

## 用法（如确需独立启动）

```bash
cd ops/examples/rabbitmq
cp .env.example .env        # 编辑账号密码
docker compose up -d        # 或: podman compose up -d
```

## 暴露端口

| 端口 | 说明 |
|------|------|
| `5673` → 5672 | AMQP 协议 |
| `15673` → 15672 | 管理台 |

> 端口可通过 `.env` 中的 `RABBITMQ_AMQP_PORT` / `RABBITMQ_MANAGEMENT_PORT` 调整。
