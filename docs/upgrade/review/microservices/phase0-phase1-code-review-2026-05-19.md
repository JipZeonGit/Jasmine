# Code Review Report

## 1. Executive Summary
- 总体评价：**存在风险**
- 核心问题概述（最关键的 3 点）
  - **运行链路没有闭环**：`microservices` 分支已经完成模块拆分，但 `Dockerfile`、`docker-compose` 仍按单体工程构建和部署，当前分支无法按现有运维脚本成功构建/启动。
  - **公共模块边界过宽**：`jasmine-common` 同时承载 WebMVC、MySQL、Flyway、Redis、RabbitMQ、Swagger、Prometheus，导致 `gateway` 被动继承不兼容技术栈和不必要的基础设施依赖。
  - **Bean 扫描与模块依赖不一致**：多个服务启动类只扫描本服务包，但业务代码已经直接注入 `jasmine-common` 与跨服务模块中的 Bean，存在高概率的运行时装配失败风险。

## 2. Critical Issues（P0 - 必须修复）
> 会导致系统错误 / 安全风险

### P0-1 当前分支的 Docker 构建与 Compose 部署链路仍是单体模式，实际已经失效

**问题说明**

Phase0 已将项目改造成 Maven 多模块，但根目录 `Dockerfile` 仍然假设源码在根 `src/`，并假设构建产物是单个 `/app/target/Jasmine-0.0.1-SNAPSHOT.jar`。而 `ops/dev/docker-compose.yml`、`ops/prod/docker-compose.yml` 仍只启动一个 `backend` 容器。

这在 `microservices` 分支上会直接导致两个问题：

1. `docker build` 时 `COPY src src` 找不到根 `src/`。
2. 即使强行修正复制路径，也不会产出单一 `jasmine.jar`，因为当前是 6 个模块。

**代码片段**

```dockerfile
# Dockerfile
COPY pom.xml ./
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B

COPY src src
RUN ./mvnw -DskipTests package -B

COPY --from=builder /app/target/Jasmine-0.0.1-SNAPSHOT.jar /app/jasmine.jar
```

```yaml
# ops/dev/docker-compose.yml
backend:
  build:
    context: ../..
    dockerfile: Dockerfile
  image: jasmine-backend:dev
```

对应位置：
- `Dockerfile:7-15`
- `Dockerfile:24`
- `ops/dev/docker-compose.yml:97-103`

**影响**

- Dev/Prod Compose 在当前分支无法代表真实运行形态。
- Phase1 的“服务注册到 Nacos”在运维层面没有可执行载体，无法形成闭环验证。
- CI/CD 仍停留在单体镜像思路，后续 Phase2 以后会持续阻塞。

**修复建议**

- 立即废弃当前“单 Dockerfile + 单 backend 容器”的微服务分支部署方式。
- 改为：
  - 每个服务独立 `Dockerfile`，或
  - 一个参数化 `Dockerfile`，通过 `ARG MODULE` 构建指定模块。
- `docker-compose` 至少拆出：
  - `jasmine-gateway`
  - `iam-service`
  - `product-service`
  - `trade-service`
  - `crm-service`

**改后代码示例**

```dockerfile
# 示例：参数化构建单个服务
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

ARG MODULE

COPY mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY pom.xml ./
COPY jasmine-common/pom.xml jasmine-common/pom.xml
COPY jasmine-gateway/pom.xml jasmine-gateway/pom.xml
COPY jasmine-iam/pom.xml jasmine-iam/pom.xml
COPY jasmine-product/pom.xml jasmine-product/pom.xml
COPY jasmine-trade/pom.xml jasmine-trade/pom.xml
COPY jasmine-crm/pom.xml jasmine-crm/pom.xml

RUN chmod +x ./mvnw && ./mvnw -pl ${MODULE} -am dependency:go-offline -B

COPY jasmine-common jasmine-common
COPY jasmine-gateway jasmine-gateway
COPY jasmine-iam jasmine-iam
COPY jasmine-product jasmine-product
COPY jasmine-trade jasmine-trade
COPY jasmine-crm jasmine-crm

RUN ./mvnw -pl ${MODULE} -am -DskipTests package -B

FROM eclipse-temurin:21-jre
WORKDIR /app
ARG MODULE
COPY --from=builder /app/${MODULE}/target/${MODULE}-0.0.1-SNAPSHOT-exec.jar /app/app.jar
ENTRYPOINT ["java","-jar","/app/app.jar"]
```

---

### P0-2 `gateway` 当前依赖图与 Spring Cloud Gateway 的 WebFlux 模型冲突

**问题说明**

`jasmine-gateway` 明确要使用 `spring-cloud-starter-gateway`，但它又直接依赖 `jasmine-common`；而 `jasmine-common` 内部引入了：

- `spring-boot-starter-web`
- `springdoc-openapi-starter-webmvc-ui`
- MySQL / Flyway / Redis / RabbitMQ

这会把 **Spring MVC Servlet 栈** 整体带进 Gateway。Spring Cloud Gateway 基于 WebFlux，和 MVC 栈不应共存。此类组合通常会在启动期直接失败，或者产生不可预测的自动配置冲突。

**代码片段**

```xml
<!-- jasmine-gateway/pom.xml -->
<dependency>
    <groupId>com.nfu</groupId>
    <artifactId>jasmine-common</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
```

```xml
<!-- jasmine-common/pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
</dependency>
```

对应位置：
- `jasmine-gateway/pom.xml:16-24`
- `jasmine-common/pom.xml:28-32`
- `jasmine-common/pom.xml:110-114`

**影响**

- `gateway` 在 Phase2 很可能一接入路由与过滤器就启动失败。
- 即使勉强启动，也会被动加载 Servlet/MVC/数据库等无关基础设施，内存、启动时延和故障面都会放大。

**修复建议**

- 把 `jasmine-common` 拆成更细粒度模块，至少分为：
  - `jasmine-common-core`：`Result`、异常、通用 DTO、JWT claims 抽象
  - `jasmine-common-service`：MyBatis/Redis/Flyway/MQ/Servlet 侧基础设施
  - `jasmine-common-gateway`：Gateway 需要的纯 WebFlux 能力
- `gateway` 改用 `springdoc-openapi-starter-webflux-ui`，不要继承 `webmvc-ui`。

**改后代码示例**

```xml
<!-- jasmine-gateway/pom.xml -->
<dependency>
    <groupId>com.nfu</groupId>
    <artifactId>jasmine-common-core</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>

<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webflux-ui</artifactId>
</dependency>
```

---

### P0-3 服务启动类的扫描边界与实际注入 Bean 不一致，存在运行时装配失败风险

**问题说明**

当前几个服务的启动类都只声明了默认 `@SpringBootApplication`，包路径分别是：

- `com.nfu.jasmine.iam`
- `com.nfu.jasmine.sales`
- `com.nfu.jasmine.vip`

这意味着 Spring 默认只扫描各自子包。但实际被注入的很多基础设施 Bean 位于兄弟包，例如：

- `com.nfu.jasmine.infra.security.CurrentUserProvider`
- `com.nfu.jasmine.infra.web.filter.RequestTraceFilter`
- `com.nfu.jasmine.infra.mq.publisher.MqMessagePublisher`

同时，`trade` / `crm` 还直接注入了跨模块 Bean：

- `trade` 注入 `com.nfu.jasmine.flower.application.support.FlowerStockService`
- `crm` 注入 `com.nfu.jasmine.vip.persistence.mapper.VipMapper`

仅靠 `@MapperScan` 只能解决 Mapper，不会解决 `@Component` / `@Service` / `@Configuration` 的扫描问题。

**代码片段**

```java
// TradeApplication
@SpringBootApplication
@MapperScan({
    "com.nfu.jasmine.sales.persistence.mapper",
    "com.nfu.jasmine.inventory.persistence.mapper",
    "com.nfu.jasmine.inventory.alert.persistence.mapper",
    "com.nfu.jasmine.flower.persistence.mapper",
    "com.nfu.jasmine.infra.outbox.persistence.mapper"
})
public class TradeApplication { }
```

```java
// SalesServiceImpl
@Autowired
private MqMessagePublisher mqMessagePublisher;

@Autowired
private FlowerStockService flowerStockService;
```

```java
// AppointmentController
@Autowired
private CurrentUserProvider currentUserProvider;
```

```java
// MySecurityConfig
@Autowired
private RequestTraceFilter requestTraceFilter;
```

对应位置：
- `jasmine-trade/src/main/java/com/nfu/jasmine/sales/TradeApplication.java:13-19`
- `jasmine-trade/src/main/java/com/nfu/jasmine/sales/application/impl/SalesServiceImpl.java:70-77`
- `jasmine-crm/src/main/java/com/nfu/jasmine/appointment/web/AppointmentController.java:29-32`
- `jasmine-iam/src/main/java/com/nfu/jasmine/config/MySecurityConfig.java:22-29`
- `jasmine-common/src/main/java/com/nfu/jasmine/infra/security/CurrentUserProvider.java:12`
- `jasmine-common/src/main/java/com/nfu/jasmine/infra/web/filter/RequestTraceFilter.java:31-33`
- `jasmine-common/src/main/java/com/nfu/jasmine/infra/mq/publisher/MqMessagePublisher.java:27`

**影响**

- 服务上下文一旦真正按微服务方式启动，极有可能出现：
  - `NoSuchBeanDefinitionException`
  - `UnsatisfiedDependencyException`
- 当前 `mvn test -DskipITs=true` 通过，并不能证明这些服务能按真实部署路径启动，因为缺少对应服务的上下文烟雾测试。

**修复建议**

- 短期止血：所有服务先统一改成 `scanBasePackages = "com.nfu.jasmine"`。
- 中期治理：把公共基础设施改造成独立 starter / auto-configuration，不再依赖“全量根包扫描”。
- 跨服务临时依赖不要直接注入实现类，至少先抽 `internal` 接口层或 Facade，避免后续 Phase3 远程化时大面积返工。

**改后代码示例**

```java
@SpringBootApplication(scanBasePackages = "com.nfu.jasmine")
@MapperScan({
    "com.nfu.jasmine.sales.persistence.mapper",
    "com.nfu.jasmine.inventory.persistence.mapper",
    "com.nfu.jasmine.inventory.alert.persistence.mapper",
    "com.nfu.jasmine.flower.persistence.mapper",
    "com.nfu.jasmine.infra.outbox.persistence.mapper"
})
public class TradeApplication {
}
```

> 说明：这只是 Phase0/1 的止血方案。长期仍建议改成显式 starter + 自动配置，而不是把所有服务都扫整个根包。

## 3. Major Issues（P1 - 高优先级）
> 影响性能或可维护性

### P1-1 `jasmine-common.yml` 把数据库/Redis 等敏感与重型配置无差别下发给所有服务，`gateway` 被迫耦合

**代码片段**

```yaml
# ops/nacos-config/jasmine-common.yml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: ${MYSQL_USER:root}
    password: ${MYSQL_PASSWORD:123456}
    url: jdbc:mysql://${MYSQL_HOST:192.168.31.26}:${MYSQL_PORT:13306}/jasmine...
  data:
    redis:
      host: ${REDIS_HOST:192.168.31.26}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:123456}
```

```yaml
# jasmine-gateway/src/main/resources/application.yml
spring:
  config:
    import:
      - nacos:jasmine-common.yml?refresh=true
      - nacos:jasmine-gateway.yml?refresh=true
```

对应位置：
- `ops/nacos-config/jasmine-common.yml:8-18`
- `jasmine-gateway/src/main/resources/application.yml:9-12`

**问题点**

- `gateway` 不应持有数据库账号、Redis 密码。
- 这违反最小权限原则。
- 也意味着 MySQL/Redis 配置错误会拖垮本不需要它们的网关启动。

**建议**

- 拆分 Nacos 配置：
  - `jasmine-observability.yml`
  - `jasmine-db-common.yml`
  - `jasmine-redis-common.yml`
  - `jasmine-mq-common.yml`
- Gateway 只导入自己真正需要的配置。

---

### P1-2 数据库迁移仍由 `iam-service` 单点承担，其他服务没有独立 schema ownership

**代码片段**

```yaml
# jasmine-iam/application.yml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
```

```yaml
# jasmine-trade/application.yml
spring:
  flyway:
    enabled: false
```

```yaml
# jasmine-crm/application.yml
spring:
  flyway:
    enabled: false
```

对应位置：
- `jasmine-iam/src/main/resources/application.yml:24-29`
- `jasmine-trade/src/main/resources/application.yml:24-25`
- `jasmine-crm/src/main/resources/application.yml:24-25`

**问题点**

- 当前还是“一个库 + 一个服务负责迁移 + 其他服务被动依赖”的模式。
- 这会让服务启动顺序对 `iam-service` 产生隐式依赖。
- 对新环境、临时环境、灰度环境都不友好。

**建议**

- 在数据库拆分前，至少增加一个独立的 schema/bootstrap 模块专门承担初始化。
- 或者短期内让每个服务携带自己所需表的迁移脚本，避免“只有 IAM 跑过 Flyway，其他服务才可用”。

---

### P1-3 `ops/dev` / `ops/prod` 还没有真正定义 5 个微服务容器，Phase1 的 Nacos 注册无法做成可重复验证

**问题说明**

Phase1 日志里写“各服务注册到 Nacos，待验证”，但当前运维编排仍只有一个 `backend` 容器，没有：

- `gateway`
- `iam-service`
- `product-service`
- `trade-service`
- `crm-service`

这意味着：

- Nacos 服务发现还停留在代码层接线；
- 编排层没有可执行验证；
- 任何“已接入 Nacos”的结论都还缺少部署级证据。

**建议**

- 在 `ops/dev/docker-compose.yml` 和 `ops/prod/docker-compose.yml` 中按真实服务拆容器。
- 为每个服务加独立 healthcheck，并在网关侧验证发现与转发。

---

### P1-4 `ops/nacos-config/import.sh` 使用原始表单拼接 YAML，配置导入存在内容损坏风险

**代码片段**

```bash
content="$(cat "$file")"

curl -s -X POST "http://$NACOS_ADDR/nacos/v1/cs/configs" \
  --user "$NACOS_USER:$NACOS_PASS" \
  -d "dataId=$data_id&group=$GROUP&tenant=$NAMESPACE&type=yaml&content=$content"
```

对应位置：
- `ops/nacos-config/import.sh:37-42`

**问题点**

- YAML 中如果出现 `&`、`+`、`%`、换行、多行注释等，表单拼接会被 URL 编码语义污染。
- 这会造成导入后的 Nacos 配置内容和文件原文不一致。

**建议**

- 使用 `--data-urlencode`，并直接从文件读取。

**改后代码示例**

```bash
curl -fsS -X POST "http://$NACOS_ADDR/nacos/v1/cs/configs" \
  --user "$NACOS_USER:$NACOS_PASS" \
  --data-urlencode "dataId=$data_id" \
  --data-urlencode "group=$GROUP" \
  --data-urlencode "tenant=$NAMESPACE" \
  --data-urlencode "type=yaml" \
  --data-urlencode "content@${file}"
```

## 4. Minor Issues（P2 - 建议优化）
> 代码风格或小问题

### P2-1 `spring.config.import` 没有使用 `optional:nacos:`，本地无 Nacos 时开发体验较差

当前各服务都写成：

```yaml
spring:
  config:
    import:
      - nacos:jasmine-common.yml?refresh=true
```

如果开发者本地没有 Nacos，服务会在配置阶段直接失败。对于 Phase0/1 这种“骨架迁移期”，建议至少给 `local` profile 留一条后路。

建议：

```yaml
spring:
  config:
    import:
      - optional:nacos:jasmine-common.yml?refresh=true
```

---

### P2-2 README、Docker 与部署说明仍大量停留在 `next` 单体语义

例如 `README.md` 仍写着：

- 当前 `next` 是主干分支
- 后端目录是 `src/main/java/`
- 后端镜像是 `jasmine-backend`

这对当前 `microservices` 分支会造成明显误导。建议为分支补一份独立的微服务 README，至少说明：

- 当前阶段只完成到 Phase1
- 各模块职责
- 现阶段哪些运维脚本仍未适配

## 5. Security Analysis

- **SQL Injection**
  - 本轮 Phase0/1 代码里，业务查询主要仍使用 MyBatis-Plus `LambdaQueryWrapper`，未看到新增的字符串拼接 SQL 注入点。
  - 风险等级：**低**

- **XSS**
  - 本轮主要是后端模块拆分与 Nacos 接入，没有新增直接的富文本/HTML 输出链路。
  - 风险等级：**低**

- **CSRF**
  - `iam-service` 当前是 JWT 无状态接口，禁用 CSRF 本身可以成立。
  - 但后续 Phase2/Phase3 如果网关统一使用 Cookie 承载 refresh token 或 SSO，需要重新审视跨站刷新与登出接口的 CSRF 防护。

- **权限绕过**
  - 当前最大风险不在 RBAC 规则本身，而在**网关与服务拓扑尚未真正建立**。
  - 现在仍是单体式 `backend` 部署，意味着“统一网关做外部入口控制”的架构目标尚未落地。

- **敏感信息泄露**
  - `gateway` 不必要地导入数据库/Redis 密码。
  - `ops/dev/docker-compose.yml` 给 `NACOS_AUTH_TOKEN` 设了硬编码默认值：

```yaml
NACOS_AUTH_TOKEN: ${NACOS_AUTH_TOKEN:-SecretToken012345678901234567890123456789012345678901234567890123456789}
```

  - `ops/nacos-config/import.sh` 使用 HTTP 明文访问 Nacos 管理接口。

- **缓存雪崩 / 穿透 / 击穿**
  - 当前缓存策略主要延续单体时期配置，Phase0/1 未看到新的微服务级隔离策略。
  - 后续如果多个服务共享同一 Redis，需要确保：
    - key 带服务前缀
    - 热点数据 TTL 与抖动策略按服务维度配置
    - 网关不要误接入业务缓存

**安全修复建议**

1. 把 `gateway` 从数据库/Redis/MQ 敏感配置中剥离。
2. Dev 环境也不要保留可直接使用的默认 `NACOS_AUTH_TOKEN`，至少改为强制从 `.env` 注入。
3. `import.sh` 支持 HTTPS，或明确限制为内网控制面使用。
4. 在 Phase2 开始前，先明确“外部访问只能走 gateway”的网络边界与端口暴露策略。

## 6. Performance Analysis

- **时间复杂度 / 空间复杂度**
  - Phase0/1 本身不是算法密集型改动，主要性能问题来自**运行时装配冗余**而非代码复杂度。

- **IO / DB 问题**
  - `gateway` 当前会被动继承数据库、Redis、Flyway 等自动装配前提，启动路径和故障面被不必要放大。
  - 所有服务统一加载 `jasmine-common.yml`，会让与当前服务无关的中间件配置也参与初始化。

- **缓存优化空间**
  - 建议尽早把缓存配置从 `jasmine-common` 中拆出来。
  - `gateway` 不应创建业务缓存。
  - 未来服务拆分后，缓存 key 应按服务域命名，避免跨服务污染。

## 7. Architecture & Design

- **是否符合 SOLID 原则**
  - 当前 **不完全符合**。
  - 最大问题是 `jasmine-common` 已演化成“God Module”，违反单一职责原则。

- **模块耦合情况**
  - 当前是“目录上拆分，运行时仍强耦合”：
    - `trade -> product`
    - `trade -> crm`
    - `crm -> iam`
  - 且耦合层级已经深入到 `Mapper` / `Service` / `Entity`，不是仅共享 DTO。

- **是否易扩展**
  - 现状下扩展成本偏高。
  - 因为一旦开始 Phase3 的远程调用改造，很多现在直接注入的类都要回退为接口契约，返工面会比较大。

## 8. Refactoring Suggestions

### 建议 1：先拆 `jasmine-common`

推荐最小拆法：

1. `jasmine-common-core`
   - `Result`
   - `ResultCode`
   - `BusinessException`
   - 通用 DTO / VO
   - JWT claims 抽象

2. `jasmine-common-service`
   - MyBatis
   - Flyway
   - Redis
   - MQ / Outbox
   - `RequestTraceFilter`
   - `CurrentUserProvider`

3. `jasmine-common-gateway`
   - Gateway 过滤器共用模型
   - WebFlux 相关配置

### 建议 2：Phase2 之前先补“启动与部署基线”

不要直接进入 Gateway 路由开发，建议先补齐：

1. 每个服务可独立构建镜像
2. 每个服务有最小 `@SpringBootTest` 上下文测试
3. Compose 可一次性拉起 5 个服务 + Nacos
4. Nacos 配置导入脚本可重复执行且不损坏 YAML

### 建议 3：为临时跨域调用增加 Facade 层

当前直接依赖：

```java
private FlowerStockService flowerStockService;
private VipMapper vipMapper;
```

建议先包一层：

```java
public interface ProductStockFacade {
    StockChangeResult adjustStock(...);
}

public interface VipReadFacade {
    VipDTO getVipById(Integer vipId);
}
```

这样在 Phase3 改成 `RestClient` 时，只替换 Facade 实现，不会把业务层全部重写。

## 9. Testability

- **是否易于测试**
  - 当前 **一般**。
  - 代码虽然已经完成模块划分，但测试体系还没有同步转成“服务级上下文验证”。

- **建议补充的测试**

1. `gateway` 上下文烟雾测试
   - 目标：确保 WebFlux 栈下可启动，且没有 MVC 冲突。

2. `iam-service` / `product-service` / `trade-service` / `crm-service` 各自的 `contextLoads`
   - 目标：验证 `CurrentUserProvider`、`MqMessagePublisher`、`RequestTraceFilter`、缓存配置、Mapper 装配都完整。

3. Docker 构建测试
   - 至少在 CI 中增加：
     - `docker build` for `gateway`
     - `docker build` for `iam-service`
     - `docker build` for `product-service`
     - `docker build` for `trade-service`
     - `docker build` for `crm-service`

4. Nacos 配置导入回归测试
   - 目标：验证导入后的 YAML 与源文件内容一致。

5. Compose 烟雾测试
   - 目标：验证 5 个服务都能注册到 Nacos，且 gateway 能发现并转发。

---

## 附：本次审查过程中的验证说明

- 已阅读：
  - `docs/project-constraints.md`
  - `docs/upgrade/logs/microservices/phase0-maven-restructure.md`
  - `docs/upgrade/logs/microservices/phase1-nacos-integration.md`
  - `docs/upgrade/plan/microservices/microservice-migration-plan.md`

- 已执行最小化测试验证：
  - `.\mvnw.cmd -q -pl jasmine-trade -am test -DskipITs=true`

- 结果说明：
  - 单元测试链路可通过。
  - 但这**不能证明微服务真实启动链路可用**，因为当前缺少 `gateway/product/trade/crm` 的启动烟雾测试，也没有按真实微服务拓扑执行 Compose 验证。
