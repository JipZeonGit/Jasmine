package com.nfu.jasmine.schema;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.core.env.Environment;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 独立数据库初始化入口（多数据源版）。
 * <p>
 * 容器化环境先运行该模块完成各微服务独立数据库的 Flyway 迁移，再启动各业务服务。
 * 每个微服务拥有独立的数据库，符合企业级微服务分库规范：
 * <ul>
 *   <li>jasmine_iam  — IAM 服务（user / role / menu / auth）</li>
 *   <li>jasmine_product — Product 服务（flower）</li>
 *   <li>jasmine_trade — Trade 服务（sales / inventory）</li>
 *   <li>jasmine_crm — CRM 服务（vip / appointment / site_message）</li>
 * </ul>
 * <p>
 * 关闭 Spring Boot Flyway 自动配置，改由本类手动对以上 4 个数据库
 * 分别执行完整的 V1~V8 迁移，每个库拥有独立的 flyway_schema_history。
 */
@SpringBootApplication(exclude = FlywayAutoConfiguration.class)
public class SchemaApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SchemaApplication.class);

    /**
     * 数据库 → Flyway 迁移脚本路径映射。
     * 每个微服务数据库独立存放迁移脚本，各自维护 flyway_schema_history。
     */
    public static final Map<String, String> DATABASES = new LinkedHashMap<>();

    static {
        DATABASES.put("jasmine_iam",     "classpath:db/migration/iam");
        DATABASES.put("jasmine_product", "classpath:db/migration/product");
        DATABASES.put("jasmine_trade",   "classpath:db/migration/trade");
        DATABASES.put("jasmine_crm",     "classpath:db/migration/crm");
    }

    private final Environment env;

    public SchemaApplication(Environment env) {
        this.env = env;
    }

    public static void main(String[] args) {
        SpringApplication.run(SchemaApplication.class, args);
    }

    @Override
    public void run(String... args) {
        String host = env.getRequiredProperty("MYSQL_HOST");
        String port = env.getProperty("MYSQL_PORT", "3306");
        String user = env.getRequiredProperty("MYSQL_USER");
        String password = env.getRequiredProperty("MYSQL_PASSWORD");

        log.info("=== Jasmine Schema 迁移开始（多数据库模式） ===");
        log.info("MySQL: {}:{}, 目标数据库: {}", host, port, DATABASES.keySet());

        int success = 0;
        for (Map.Entry<String, String> entry : DATABASES.entrySet()) {
            String dbName = entry.getKey();
            String url = String.format(
                    "jdbc:mysql://%s:%s/%s?useUnicode=true&characterEncoding=UTF-8&connectionCollation=utf8mb4_0900_ai_ci&serverTimezone=Asia/Shanghai",
                    host, port, dbName);

            log.info("--- [{}] 开始迁移 (路径: {}) ---", dbName, entry.getValue());
            try {
                Flyway flyway = Flyway.configure()
                        .dataSource(url, user, password)
                        .locations(entry.getValue())
                        .baselineOnMigrate(true)
                        .baselineVersion("1")
                        .cleanDisabled(true)
                        .load();

                var result = flyway.migrate();
                log.info(">>> [{}] 迁移完成，当前版本: {}", dbName,
                        result.targetSchemaVersion != null ? result.targetSchemaVersion : "baseline");
                success++;
            } catch (Exception e) {
                log.error("!!! [{}] 迁移失败: {}", dbName, e.getMessage(), e);
                // 不立即退出，继续尝试其他数据库，最后汇总报错
            }
        }

        log.info("=== 迁移汇总: {}/{} 数据库成功 ===", success, DATABASES.size());
        if (success < DATABASES.size()) {
            // 任一数据库迁移失败则返回非零退出码，让容器编排感知
            throw new RuntimeException("部分数据库迁移失败，请检查日志");
        }
    }
}
