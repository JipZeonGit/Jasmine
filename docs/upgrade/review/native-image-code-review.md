# PR20 GraalVM Native Image 部署链路代码审查报告

## 审查范围

| 文件 | 类型 |
|:---|:---|
| [JasmineNativeImageHints.java](../../../src/main/java/com/nfu/jasmine/config/JasmineNativeImageHints.java) | 反射提示注册 |
| [Dockerfile.native](../../../Dockerfile.native) | Native Image 构建镜像 |
| [Dockerfile](../../../Dockerfile) | JVM 构建镜像 |
| [docker-publish.yml](../../../.github/workflows/docker-publish.yml) | CI/CD 工作流 |
| [.env.example](../../../ops/.env.example) | 环境变量模板 |
| [dev/docker-compose.yml](../../../ops/dev/docker-compose.yml) | 开发环境编排 |
| [prod/docker-compose.yml](../../../ops/prod/docker-compose.yml) | 生产环境编排 |
| [pom.xml](../../../pom.xml) | Maven 构建配置 |
| [JasmineApplication.java](../../../src/main/java/com/nfu/jasmine/JasmineApplication.java) | 主应用入口 |

## 错误根因回顾

之前部署 Native Image 时遇到的报错核心链路：

```
ExceptionInInitializerError
  → ClassPathMapperScanner.<clinit>
    → LoggerFactory.getLogger
      → LogFactory.getLog
        → NullPointerException
```

**根因**：MyBatis 的 `LogFactory` 在类初始化阶段（`<clinit>`）通过反射尝试加载日志实现类 `Slf4jImpl`。在 GraalVM Native Image 中，这个反射路径如果没有被显式注册，`Class.forName("org.apache.ibatis.logging.slf4j.Slf4jImpl")` 会返回 null 或抛出异常，导致 `LogFactory.getLog()` 在第 54 行产生 NPE。

---

## 一、JasmineNativeImageHints.java — ✅ 通过（附小建议）

### 1.1 日志适配器反射 — ✅ 已修复

```java
// 第 188-189 行
registerTypeQuietly(hints, "org.apache.ibatis.logging.slf4j.Slf4jImpl", FULL_ACCESS);
registerTypeQuietly(hints, "org.apache.ibatis.logging.LogFactory", FULL_ACCESS);
```

**审查结论**：这正是解决 `NullPointerException at LogFactory.getLog(LogFactory.java:54)` 的关键修复。`Slf4jImpl` 和 `LogFactory` 都被注册了 `FULL_ACCESS`（含 `INVOKE_DECLARED_CONSTRUCTORS`），可以确保反射实例化和静态初始化都能正常进行。

### 1.2 Mapper 代理 — ✅ 覆盖完整

第 149-168 行列出的 15 个 Mapper 接口全部通过 `hints.proxies().registerJdkProxy()` 注册了动态代理提示。

### 1.3 实体类反射 — ✅ 覆盖完整

第 73-89 行覆盖了所有 15 个 MyBatis-Plus 实体类（包括 PR18/PR19 新增的 `SiteMessage`、`EventOutbox`、`InventoryAlert`）。

### 1.4 MQ 消息体 — ✅ 覆盖完整

第 104-108 行覆盖了全部 4 种 MQ 消息类型。Jackson 反序列化需要的反射元数据已注册。

### 1.5 资源文件 — ✅ 覆盖完整

第 247-253 行注册了 `mapper/**/*.xml`、`db/migration/**/*.sql`、`application*.yml`、`logback-spring.xml` 等运行时资源。

> [!TIP]
> **小建议 1**：考虑补充 `org.apache.ibatis.logging.stdout.StdOutImpl` 的反射注册。虽然项目当前用的是 Slf4j，但如果未来某天 debug 时临时切到 StdOut 日志，没有注册会直接炸掉。
>
> **小建议 2**：第 209 行 `org.apache.ibatis.reflection.property.BeanUtil` 在 MyBatis 3.5.x 中**不存在**。`registerTypeQuietly` 会优雅地吞掉 `ClassNotFoundException`，所以不会报错，但它是一行**无效代码**，建议清理。

---

## 二、Dockerfile.native — ✅ 通过（附中风险建议）

```dockerfile
FROM ghcr.io/graalvm/native-image-community:21 AS builder
...
RUN ./mvnw -Pnative -DskipTests package -B
...
FROM debian:bookworm-slim
...
COPY --from=builder /app/target/jasmine-native /app/jasmine-native
ENTRYPOINT ["/app/jasmine-native"]
```

**审查结论**：两阶段构建设计正确。`-Pnative` 激活了 `pom.xml` 中的 native profile，`native-maven-plugin` 的 `compile-no-fork` 目标会在 package 阶段生成 `jasmine-native` 二进制。

> [!WARNING]
> **中风险**：`debian:bookworm-slim` 运行阶段缺少 `libz.so`。GraalVM Native Image 默认静态链接 zlib，但如果项目的依赖（如某些 JDBC driver 或 compression library）依赖动态链接的 `libz`，会在运行时报 `UnsatisfiedLinkError`。建议在运行阶段加装 `zlib1g`：
> ```dockerfile
> RUN apt-get update && apt-get install -y --no-install-recommends curl zlib1g && rm -rf /var/lib/apt/lists/*
> ```
> 如果部署后没遇到这个问题，那说明当前依赖不需要，可以忽略。

---

## 三、Dockerfile（JVM 版）— ✅ 通过

标准的两阶段构建，无问题。`eclipse-temurin:21-jdk` 构建 → `eclipse-temurin:21-jre` 运行 → jar 包入口。

---

## 四、docker-publish.yml — ✅ 通过（附一个建议）

### 4.1 双镜像构建 — ✅ 设计合理

Step 5 构建 JVM 版（`Dockerfile`），Step 6 构建 GraalVM 版（`Dockerfile.native`），Step 7 构建前端。三个 Job 都启用了 `cache-from/cache-to: type=gha` GitHub Actions 缓存加速。

### 4.2 标签策略 — ✅ 正确

- `next` 分支（当前主干）：`next-latest` / `next-<sha>`
- `main` 分支（假设未来改回 main）：`latest` / `sha-<sha>`

### 4.3 paths-ignore — ✅ 已加入

`**.md` 文件修改不触发工作流。

> [!NOTE]
> **建议**：注意 Step 5 和 Step 6 是**串行的**（同一个 Job 内的 steps）。GraalVM Native Image 编译在 GitHub Actions 的免费 runner 上（2 核 7GB）**极其缓慢**，通常需要 15-30 分钟甚至更久，而且内存压力很大，可能会 OOM。如果遇到了 CI 超时或者内存不足，考虑以下方案之一：
> - 将 GraalVM 构建拆到一个单独的 Job，加 `continue-on-error: true`，避免阻塞 JVM 版构建。
> - 或者在 native-maven-plugin 配置里加 `-J-Xmx4g` 限制编译器内存。

---

## 五、.env.example — ✅ 通过

第 10-15 行清晰地给出了 JVM 版和 GraalVM 版的切换说明：

```env
# JVM 版（稳定，镜像较大 ~300MB，推荐生产使用）
BACKEND_IMAGE=ghcr.io/jipzeongit/jasmine-backend
# GraalVM Native Image 版（启动快、镜像小 ~150MB，但 MyBatis-Plus 反射兼容性需关注）
# 如需使用 native 版本，将 BACKEND_IMAGE 替换为：
# BACKEND_IMAGE=ghcr.io/jipzeongit/jasmine-backend-graalvm
```

**审查结论**：文档和默认值都设计得非常好。默认走稳定的 JVM 版，想试 Native Image 只需要注释一行取消注释一行。

---

## 六、dev/docker-compose.yml — ✅ 通过

dev 环境使用 `build: context: ../..` 本地构建，引用的是 `Dockerfile`（JVM 版），不涉及 Native Image，无风险。

---

## 七、prod/docker-compose.yml — ✅ 通过

```yaml
backend:
  image: ${BACKEND_IMAGE}:${BACKEND_IMAGE_TAG}
  pull_policy: always
```

**审查结论**：通过环境变量注入镜像名和标签。当用户在 `.env` 中把 `BACKEND_IMAGE` 切换为 `jasmine-backend-graalvm` 时，会无缝拉取 Native Image 版镜像。`pull_policy: always` 确保每次 `docker compose up` 都拉最新版。设计合理。

---

## 八、pom.xml — ✅ 通过

```xml
<profile>
    <id>native</id>
    <build>
        <plugins>
            <plugin>
                <groupId>org.graalvm.buildtools</groupId>
                <artifactId>native-maven-plugin</artifactId>
                <configuration>
                    <imageName>jasmine-native</imageName>
                    <buildArgs>
                        <buildArg>-H:+ReportExceptionStackTraces</buildArg>
                    </buildArgs>
                </configuration>
            </plugin>
        </plugins>
    </build>
</profile>
```

**审查结论**：`compile-no-fork` 目标在 `package` 阶段执行，与 `Dockerfile.native` 中的 `./mvnw -Pnative -DskipTests package -B` 完美匹配。`-H:+ReportExceptionStackTraces` 是调试 Native Image 启动失败时的关键 flag，保留它非常正确。

> [!NOTE]
> `native-maven-plugin` 没有显式指定版本号。这是因为 `spring-boot-starter-parent 3.5.13` 已经在其 BOM 中管理了 `org.graalvm.buildtools:native-maven-plugin` 的版本。这是正确做法，保持跟随 Boot 版本即可。

---

## 九、JasmineApplication.java — ✅ 通过

```java
@ImportRuntimeHints(JasmineNativeImageHints.class)
public class JasmineApplication { ... }
```

**审查结论**：`@ImportRuntimeHints` 注解正确挂载在主应用类上，Spring AOT 编译时会自动调用 `JasmineNativeImageHints.registerHints()` 生成 `reflect-config.json` 等元数据文件。这是 Spring Boot 3 官方推荐的 Native Image 提示注册方式。

---

## 综合审查结论

| 维度 | 状态 | 说明 |
|:---|:---|:---|
| **根因修复** | ✅ **已解决** | `Slf4jImpl` 和 `LogFactory` 的反射注册已到位，`NullPointerException` 问题应被彻底消除 |
| **实体/Mapper 覆盖** | ✅ **完整** | 全部 15 个实体 + 15 个 Mapper 代理 + 4 种 MQ 消息 + 枚举均已覆盖 |
| **Dockerfile 设计** | ✅ **合理** | JVM / Native 双轨并行，两阶段构建 |
| **CI/CD 流程** | ✅ **完整** | 双镜像构建 + GitHub 缓存加速 + MD 文件过滤 |
| **Ops 切换方案** | ✅ **灵活** | 通过 `.env` 的 `BACKEND_IMAGE` 一行切换 JVM / Native |
| **已知风险** | ⚠️ | GraalVM 构建在免费 CI runner 上可能超时或 OOM |

> [!IMPORTANT]
> 整体链路设计极其扎实。项目遇到的那个启动崩溃问题（MyBatis LogFactory NPE）的根因已经被精准修复。建议在下次 push 触发 CI 构建完成后，再次在 Docker 环境中用 `jasmine-backend-graalvm:next-latest` 镜像做一次实际启动验证，确认闭环。
