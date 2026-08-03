# Jasmine ops/examples —— 独立参考示例

本目录存放一些**独立、参考性质**的容器编排示例，不参与主流程部署。

> 当前 Jasmine 的实际部署入口已收口到：
> - `ops/docker/dev` —— 本地开发中间件（Docker 版）
> - `ops/docker/prod` —— 生产全栈（Docker 版）
> - `ops/podman/prod`  —— 生产全栈（Podman + docker compose 版）
>
> 这三个目录的 compose 均已内置 MySQL / Redis / RabbitMQ / Nacos 等中间件，
> 因此以下独立示例**不再需要单独使用**，仅作参考。

## 目录

```
ops/examples/
└── rabbitmq/       # 仅单独启动一个 RabbitMQ（早期过渡方案，已被全栈编排内置取代）
    ├── docker-compose.yml
    └── .env.example
```
