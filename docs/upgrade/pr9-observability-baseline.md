# PR9 日志与指标基线

## 本轮目标

对齐项目当前目标栈里的 Logback + 统一日志 + Actuator + Micrometer，让 Jasmine 在不引入重型运维平台的前提下先具备基础可观测性。

## 本轮内容

1. 增加 `micrometer-registry-prometheus` 依赖
2. 统一 Logback 控制台日志格式，并把 `traceId / requestId` 放进 MDC
3. 新增 `RequestTraceFilter`，统一补齐请求标识并输出访问日志
4. 统一全局异常日志格式，避免只返回错误码不留排查线索
5. 为登录、刷新、注销、改密等关键认证链路补充基础业务日志
6. 开启 Actuator / Micrometer / Prometheus 相关配置
7. 暴露基础观测端点：
   - `/actuator/health`
   - `/actuator/info`
   - `/actuator/metrics`
   - `/actuator/prometheus`

## 预期效果

- 控制台日志格式统一
- 每个业务请求都有 `traceId / requestId`
- Prometheus 可以抓取基础指标
- JVM / HTTP / HikariCP 数据源指标默认可见
- 关键异常会在日志中带上请求方法与 URI

## 说明

这轮只做日志与指标基线，不与数据库升级、SQL 兼容性整理或业务模型重构混在一起。
## 本地测试记录

本轮在项目默认地址下完成了本地联调验证，结果如下：

1. `GET /actuator/health` 返回 `200`，状态为 `UP`
2. `GET /actuator/prometheus` 返回 `200`，Prometheus 文本输出中可见：
   - `jvm_memory_used_bytes`
   - `http_server_requests`
   - `hikaricp_connections`
3. 登录后访问 `GET /actuator/metrics/http.server.requests` 与 `GET /actuator/metrics/hikaricp.connections`，指标细项可正常返回
4. 带 `X-Trace-Id` / `X-Request-Id` 访问 `GET /user/info`，响应头会原样回传请求标识
5. 控制台访问日志已验证包含：方法、URI、状态码、耗时、客户端 IP，以及 `traceId / requestId`
6. 参数校验失败场景下，全局异常日志能够记录请求方法、URI 与错误信息
7. Windows PowerShell 控制台编码已调整为 UTF-8，中文日志与编译输出显示正常