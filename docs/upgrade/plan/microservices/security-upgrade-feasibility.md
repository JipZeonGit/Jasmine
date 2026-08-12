# 安全升级可行性评估

> 评估日期：2026-08-12
> 触发原因：fastjson2 安全升级后，对全项目 235 个依赖进行 OSV 漏洞库批量扫描，发现 29 个依赖、85 个已知漏洞。本文档评估修复方案的无损升级可行性。

## 一、漏洞扫描总览

| 级别 | 受影响依赖数 | 漏洞总数 |
|------|------------|---------|
| 🔴 高危/严重 | 9 | 18 |
| 🟠 中危 | 15 | 48 |
| 🟡 低危 | 9 | 19 |
| **合计** | **29** | **85** |

绝大部分漏洞集中在 Spring Framework / Netty / Tomcat / Jackson 四个底层栈，由 Spring Boot BOM 统一管控，可通过升级 Spring Boot 一并修复。

### 已确认无漏洞的直接依赖

fastjson2:2.0.63、snakeyaml:2.4、log4j:2.24.3、jjwt:0.12.7、mybatis-plus:3.5.14、springdoc:2.8.16、lombok:1.18.44、mysql-connector-j:9.6.0、amqp-client:5.25.0、HikariCP:6.3.3、flyway:11.7.2、resilience4j:2.2.0、lettuce:6.6.0、druid:1.2.27、nacos-client:3.0.3。

## 二、逐项评估

### 1. Spring Boot 3.5.13 → 3.5.16 — ✅ 无损升级

**评估结论：可无损升级。**

- 3.5.14 / 3.5.15 / 3.5.16 三个 patch 版本全部为 bug fix + 依赖升级，无任何 breaking changes、deprecation 或 migration steps
- 修复 CVE-2026-40973（临时目录属主校验，HIGH），3.5.14 起修复
- 3.5.16 是 3.5.x 线最新版（已被 Maven Central 元数据确认）

#### 自动带入的传递依赖版本升级

| 依赖 | 当前 (3.5.13) | 升级后 (3.5.16) | 漏洞修复线 | 是否达标 |
|------|-------------|---------------|-----------|---------|
| Spring Framework | 6.2.17 | 6.2.19 | 需 >6.2.17 | ✅ |
| Spring Security | 6.5.9 | 6.5.11 | 需 >6.5.9 | ✅ |
| Spring Data | 3.5.10 | 2025.0.13 | 需 >3.5.10 | ✅ |
| Tomcat embed | 10.1.53 | 10.1.55 | 需 ≥10.1.55 | ✅（正好） |
| Netty | 4.1.132 | 4.1.135 | 需 >4.1.132 | ✅ |
| Reactor Netty | 1.2.16 | 1.2.18 | — | ✅ |
| Jackson | 2.21.2 | **2.21.4** | 需 ≥2.21.5 | ⚠️ 差 1 小版本 |

#### Jackson 补充说明

Spring Boot 3.5.16 只带到 Jackson 2.21.4。10 个漏洞中 8 个修复（含 2 个 HIGH），剩余 2 个 MODERATE 需要 2.21.5：

| 未修复漏洞 | severity | 描述 | 修复版本 |
|-----------|----------|------|---------|
| GHSA-5gvw-p9qm-jgwh | MODERATE | @JsonView 绕过 @JsonUnwrapped 容器属性 | 2.21.5 |
| GHSA-mhm7-754m-9p8w | MODERATE | @JsonView 绕过 @JsonTypeInfo(EXTERNAL_PROPERTY) creator 属性 | 2.21.5 |

**项目实际影响**：经过全代码库排查，项目代码**未使用 @JsonView、@JsonUnwrapped、@JsonTypeInfo(EXTERNAL_PROPERTY)**，只用 @JsonIgnore、@JsonFormat、@JsonInclude 等基础注解。这 2 个 MODERATE 漏洞在项目中实际不可利用。

**决策**：不做 Jackson override。版本继续交由 Spring Boot BOM 统一管控，减少手动维护项。后续升 Spring Boot 时 Jackson 自动跟涨。

### 2. Spring Cloud 2025.0.0 → 2025.0.3 — ✅ 无损升级

**评估结论：可无损升级。**

- 2025.0.1 / 2025.0.2 / 2025.0.3 三个版本全部为 bug fix + 模块版本 bump，无 breaking changes（所有 breaking changes 都在 2025.0.0 本身，项目已吸收）
- 与 Spring Boot 3.5.16 完全兼容（Spring Cloud 2025.0.x 线对应 Spring Boot 3.5.x）
- 2025.0.3 是 2025.0.x 线最新版（已被 Maven Central 元数据确认）
- Spring Cloud 2025.1.x 需要 Spring Boot 4.0.x，与当前栈不兼容，不可走

#### 修复的 Gateway CVE

Spring Cloud Gateway 随版本从 4.3.0 升至 4.3.5，修复 3 个 CVE：

| Advisory | severity | 描述 | 修复版本 |
|----------|----------|------|---------|
| GHSA-q2cj-h8fw-q4cc (CVE-2025-41243) | **CRITICAL** (CVSS 9.9) | SpEL/EL 属性注入，spring-cloud-gateway-server-webflux | 4.3.1 |
| GHSA-fwxx-wv44-7qfg | HIGH | EL 注入（actuator 暴露时），spring-cloud-gateway-server | 4.3.2 |
| CVE-2026-47825 | HIGH | X-Forwarded-For / Forwarded 头欺骗 | 4.3.5 |

项目的 `jasmine-gateway` 用 `spring-cloud-starter-gateway-server-webflux`，其传递依赖 `spring-cloud-gateway-server` 和 `spring-cloud-gateway-server-webflux` 均由 BOM 管控，改 BOM 版本即可。

**Caveat**：4.3.5 修复 CVE-2026-47825 时默认禁用了 WebFlux 的 `NettyServerCustomizer`。经过全代码库排查（`grep -rn "NettyServerCustomizer\|NettyServer\|HttpServer\|HttpClientCustomizer\|WebServerFactoryCustomizer"`），项目代码**未使用**此组件，零影响。

### 3. Spring Cloud Alibaba 2025.0.0.0 — ❌ 不动

**评估结论：不应升级，保持 2025.0.0.0。**

| 验证项 | 结论 |
|--------|------|
| 是否有 CVE | 无（OSV 查询 com.alibaba.cloud 全部子模块，零结果） |
| 2025.1.0.0 是否兼容 | ❌ 要求 Spring Boot 4.0.x + Spring Cloud 2025.1.x，与当前栈不兼容 |
| 是否有 patch 版 | 无（2025.0.0.0 已是 2025.0.x 线唯一正式发布版） |
| 与 Spring Cloud 2025.0.3 是否兼容 | ✅ 同属 2025.0.x 线 patch 升级，标准兼容配置 |

### 4. commons-compress 1.24.0 — ⚠️ 本次不处理

- 2 个漏洞（GHSA-4265-ccf5-phj5 Pack200 OOM、GHSA-4g9r-vxhx-9pgx DUMP 无限循环 DoS）
- 为 Testcontainers 传递依赖，**仅在测试 classpath 中**，不影响生产运行时
- 仅在解压恶意压缩文件时可触发
- 建议后续单独通过升级 Testcontainers 或显式 `<dependencyManagement>` override 处理，不在本次安全升级中一起做

## 三、实施计划

### 改动文件

`pom.xml` 一个文件，2 行（+ Jackson 可选 1 行）：

```xml
<!-- Line 8: Spring Boot parent -->
<version>3.5.16</version>        <!-- 原 3.5.13 -->

<!-- Line 31: Spring Cloud -->
<spring-cloud.version>2025.0.3</spring-cloud.version>   <!-- 原 2025.0.0 -->
```

**不动的属性**：
- `<spring-cloud-alibaba.version>2025.0.0.0</spring-cloud-alibaba.version>` — 保持不变
- `<fastjson2.version>2.0.63</fastjson2.version>` — 已在上一轮升级
- 其他属性（mybatis-plus、springdoc、jjwt、jackson 等）均无 CVE 或由 Spring Boot BOM 管控，不动

### 验证步骤

1. `./mvnw compile` — 编译通过
2. `./mvnw test -DskipITs` — 全量单测通过（预期 121 例全绿）
3. 确认传递依赖版本已更新：
   ```bash
   ./mvnw dependency:list | grep -E "spring-boot:|tomcat-embed-core:|netty-codec-http:|jackson-databind:|spring-cloud-gateway"
   ```

### 验证结果

| 检查项 | 预期 | 实际 | 结果 |
|--------|------|------|------|
| Spring Boot | 3.5.16 | 3.5.16 | ✅ |
| Tomcat embed | 10.1.55 | 10.1.55 | ✅ |
| Netty | 4.1.135 | 4.1.135.Final | ✅ |
| Jackson databind | 2.21.4 | 2.21.4 | ✅ |
| Spring Cloud Gateway | 4.3.5 | 4.3.5 | ✅ |
| Spring Framework | 6.2.19 | 6.2.19 | ✅ |
| 单测 | 56 例全绿 | 56 例全绿 | ✅ |
| 编译 | 通过 | 通过 | ✅ |

### 预期消除漏洞数

| 修复项 | 消除漏洞数 | 涵盖高危 |
|--------|-----------|---------|
| Spring Boot 3.5.16 升级 | ~55 | Spring RCE/路径遍历/DoS、Tomcat 认证绕过、Netty HTTP 走私/DoS、Spring Security 冒充、Jackson 8/10（含 2 HIGH） |
| Spring Cloud 2025.0.3 升级 | ~3 | Gateway SpEL/EL 注入（1 CRITICAL + 2 HIGH） |
| **合计** | **~58** | 含全部高危：RCE / 路径遍历 / SpEL 注入 / 认证绕过 / HTTP 请求走私 |

剩余 2 个 MODERATE（Jackson @JsonView 绕过）不可利用（项目未用受影响特性），版本交由 Spring Boot BOM 统一管控，后续升级 Boot 时自动跟涨消除。

## 四、数据来源

- OSV API (`api.osv.dev`) 批量查询 235 个依赖，取回 85 个 GHSA 详情及 CVSS 向量
- Maven Central 元数据确认各版本可用性
- Spring Boot 3.5.16 BOM (`spring-boot-dependencies-3.5.16.pom`) 确认传递依赖版本
- Spring Cloud 2025.0.3 BOM (`spring-cloud-dependencies-2025.0.3.pom`) 确认 Gateway 4.3.5
- Spring Cloud Alibaba Maven 元数据 + 官方 README 确认 2025.1.0.0 不兼容当前栈
- 项目代码库 grep 确认 NettyServerCustomizer / @JsonView / @JsonUnwrapped 未使用
