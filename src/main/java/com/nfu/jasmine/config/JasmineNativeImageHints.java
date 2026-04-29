package com.nfu.jasmine.config;

import com.nfu.jasmine.appointment.model.entity.Appointment;
import com.nfu.jasmine.common.enums.ResultCode;
import com.nfu.jasmine.common.utils.JwtTokenClaims;
import com.nfu.jasmine.common.vo.Result;
import com.nfu.jasmine.flower.model.entity.Flower;
import com.nfu.jasmine.iam.model.entity.AuthRefreshToken;
import com.nfu.jasmine.iam.model.entity.Menu;
import com.nfu.jasmine.iam.model.entity.Role;
import com.nfu.jasmine.iam.model.entity.RoleMenu;
import com.nfu.jasmine.iam.model.entity.User;
import com.nfu.jasmine.iam.model.entity.UserRole;
import com.nfu.jasmine.infra.mq.message.AccessLogMessage;
import com.nfu.jasmine.infra.mq.message.AppointmentCreatedMessage;
import com.nfu.jasmine.infra.mq.message.InventoryChangedMessage;
import com.nfu.jasmine.infra.mq.message.SalesCreatedMessage;
import com.nfu.jasmine.infra.notification.model.entity.SiteMessage;
import com.nfu.jasmine.infra.outbox.model.entity.EventOutbox;
import com.nfu.jasmine.infra.outbox.model.enums.OutboxStatus;
import com.nfu.jasmine.inventory.alert.model.entity.InventoryAlert;
import com.nfu.jasmine.inventory.alert.model.enums.AlertStatus;
import com.nfu.jasmine.inventory.model.entity.Inventory;
import com.nfu.jasmine.inventory.model.enumtype.InventoryBizType;
import com.nfu.jasmine.sales.model.entity.Sales;
import com.nfu.jasmine.sales.model.entity.SalesItem;
import com.nfu.jasmine.vip.model.entity.Vip;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.lang.NonNull;

/**
 * GraalVM Native Image 运行时提示注册器。
 * <p>
 * 为 MyBatis-Plus 实体反射、MQ 消息序列化、fastjson2、JJWT 等
 * 依赖运行时反射的组件统一注册可达性元数据。
 */
public class JasmineNativeImageHints implements RuntimeHintsRegistrar {

    private static final MemberCategory[] ENTITY_CATEGORIES = {
            MemberCategory.INVOKE_PUBLIC_METHODS,
            MemberCategory.INVOKE_DECLARED_METHODS,
            MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
            MemberCategory.DECLARED_FIELDS,
            MemberCategory.PUBLIC_FIELDS
    };

    private static final MemberCategory[] BEAN_CATEGORIES = {
            MemberCategory.INVOKE_PUBLIC_METHODS,
            MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
            MemberCategory.DECLARED_FIELDS
    };

    private static final MemberCategory[] FULL_ACCESS = MemberCategory.values();

    @Override
    public void registerHints(@NonNull RuntimeHints hints, ClassLoader classLoader) {
        registerEntities(hints);
        registerMqMessages(hints);
        registerEnums(hints);
        registerSecurityClasses(hints);
        registerMyBatisProxies(hints);
        registerMyBatisInternals(hints);
        registerMyBatisResources(hints);
    }

    /**
     * 注册 MyBatis-Plus 实体类的反射提示（含字段读写）。
     * MyBatis-Plus 运行时通过反射读写字段值、构建 Wrapper 条件。
     */
    private void registerEntities(RuntimeHints hints) {
        Class<?>[] entities = {
                Flower.class,
                Sales.class,
                SalesItem.class,
                Inventory.class,
                InventoryAlert.class,
                Appointment.class,
                User.class,
                Role.class,
                Menu.class,
                UserRole.class,
                RoleMenu.class,
                AuthRefreshToken.class,
                Vip.class,
                EventOutbox.class,
                SiteMessage.class
        };
        for (Class<?> entity : entities) {
            hints.reflection().registerType(entity, ENTITY_CATEGORIES);
        }
        // Result<T> 泛型容器被 Jackson / fastjson2 反射访问
        hints.reflection().registerType(Result.class, BEAN_CATEGORIES);
        // JwtTokenClaims 被 JJWT 解析后反射构建
        hints.reflection().registerType(JwtTokenClaims.class, BEAN_CATEGORIES);
    }

    /**
     * 注册 MQ 消息体的反射提示。
     * RabbitMQ Jackson2JsonMessageConverter 反序列化时需要反射访问。
     */
    private void registerMqMessages(RuntimeHints hints) {
        Class<?>[] messages = {
                SalesCreatedMessage.class,
                InventoryChangedMessage.class,
                AppointmentCreatedMessage.class,
                AccessLogMessage.class
        };
        for (Class<?> message : messages) {
            hints.reflection().registerType(message, BEAN_CATEGORIES);
        }
    }

    /**
     * 注册枚举类反射提示。
     * MyBatis-Plus / Jackson 反序列化枚举值时需要反射。
     */
    private void registerEnums(RuntimeHints hints) {
        Class<?>[] enums = {
                ResultCode.class,
                OutboxStatus.class,
                AlertStatus.class,
                InventoryBizType.class
        };
        for (Class<?> enumClass : enums) {
            hints.reflection().registerType(enumClass,
                    MemberCategory.INVOKE_PUBLIC_METHODS,
                    MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
        }
    }

    /**
     * 注册安全相关类的反射提示。
     * BCryptPasswordEncoder / JJWT 内部使用反射。
     */
    private void registerSecurityClasses(RuntimeHints hints) {
        hints.reflection().registerType(
                org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder.class,
                MemberCategory.INVOKE_PUBLIC_METHODS,
                MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
    }

    /**
     * 注册 MyBatis Mapper 接口的动态代理提示。
     * MyBatis-Plus 通过 JDK 动态代理生成 Mapper 实现类。
     */
    private void registerMyBatisProxies(RuntimeHints hints) {
        Class<?>[] mapperInterfaces = {
                com.nfu.jasmine.flower.persistence.mapper.FlowerMapper.class,
                com.nfu.jasmine.sales.persistence.mapper.SalesMapper.class,
                com.nfu.jasmine.sales.persistence.mapper.SalesItemMapper.class,
                com.nfu.jasmine.inventory.persistence.mapper.InventoryMapper.class,
                com.nfu.jasmine.inventory.alert.persistence.mapper.InventoryAlertMapper.class,
                com.nfu.jasmine.appointment.persistence.mapper.AppointmentMapper.class,
                com.nfu.jasmine.iam.persistence.mapper.UserMapper.class,
                com.nfu.jasmine.iam.persistence.mapper.RoleMapper.class,
                com.nfu.jasmine.iam.persistence.mapper.MenuMapper.class,
                com.nfu.jasmine.iam.persistence.mapper.UserRoleMapper.class,
                com.nfu.jasmine.iam.persistence.mapper.RoleMenuMapper.class,
                com.nfu.jasmine.iam.persistence.mapper.AuthRefreshTokenMapper.class,
                com.nfu.jasmine.vip.persistence.mapper.VipMapper.class,
                com.nfu.jasmine.infra.outbox.persistence.mapper.EventOutboxMapper.class,
                com.nfu.jasmine.infra.notification.persistence.mapper.SiteMessageMapper.class
        };
        for (Class<?> mapper : mapperInterfaces) {
            hints.proxies().registerJdkProxy(mapper);
        }
    }

    /**
     * 注册 MyBatis / MyBatis-Plus 内部核心类的反射提示。
     * <p>
     * 这些类大量使用反射，GraalVM Native Image 默认不允许反射访问，
     * 必须显式注册才能在 native image 中正常运行。
     * <ul>
     *   <li>LogFactory / Slf4jImpl — 日志适配器反射加载</li>
     *   <li>MapperProxy / MapperMethod — Mapper 动态代理核心</li>
     *   <li>MybatisMapperProxy — MyBatis-Plus 增强代理</li>
     *   <li>Reflector / MetaClass — 实体反射元数据</li>
     *   <li>TypeHandler 体系 — 类型转换反射</li>
     *   <li>TableInfoHelper — 表元数据缓存</li>
     * </ul>
     */
    private void registerMyBatisInternals(RuntimeHints hints) {
        // ---- MyBatis 日志适配器 ----
        // LogFactory.setImplementation() 通过反射实例化日志实现类
        registerTypeQuietly(hints, "org.apache.ibatis.logging.slf4j.Slf4jImpl", FULL_ACCESS);
        registerTypeQuietly(hints, "org.apache.ibatis.logging.LogFactory", FULL_ACCESS);

        // ---- MyBatis Mapper 代理核心 ----
        registerTypeQuietly(hints, "org.apache.ibatis.binding.MapperProxy", FULL_ACCESS);
        registerTypeQuietly(hints, "org.apache.ibatis.binding.MapperMethod", FULL_ACCESS);
        registerTypeQuietly(hints, "org.apache.ibatis.binding.MapperMethod$SqlCommand", FULL_ACCESS);
        registerTypeQuietly(hints, "org.apache.ibatis.binding.MapperMethod$MethodSignature", FULL_ACCESS);

        // ---- MyBatis-Plus Mapper 代理 ----
        registerTypeQuietly(hints, "com.baomidou.mybatisplus.core.MybatisMapperProxy", FULL_ACCESS);
        registerTypeQuietly(hints, "com.baomidou.mybatisplus.core.MybatisMapperMethod", FULL_ACCESS);

        // ---- MyBatis 反射工具 ----
        registerTypeQuietly(hints, "org.apache.ibatis.reflection.Reflector", FULL_ACCESS);
        registerTypeQuietly(hints, "org.apache.ibatis.reflection.MetaClass", FULL_ACCESS);
        registerTypeQuietly(hints, "org.apache.ibatis.reflection.MetaObject", FULL_ACCESS);
        registerTypeQuietly(hints, "org.apache.ibatis.reflection.SystemMetaObject", FULL_ACCESS);
        registerTypeQuietly(hints, "org.apache.ibatis.reflection.DefaultReflectorFactory", FULL_ACCESS);
        registerTypeQuietly(hints, "org.apache.ibatis.reflection.factory.DefaultObjectFactory", FULL_ACCESS);
        registerTypeQuietly(hints, "org.apache.ibatis.reflection.wrapper.DefaultObjectWrapperFactory", FULL_ACCESS);

        // ---- MyBatis 类型处理器 ----
        registerTypeQuietly(hints, "org.apache.ibatis.type.TypeHandlerRegistry", FULL_ACCESS);
        registerTypeQuietly(hints, "org.apache.ibatis.type.TypeAliasRegistry", FULL_ACCESS);

        // ---- MyBatis-Plus 表元数据 ----
        registerTypeQuietly(hints, "com.baomidou.mybatisplus.core.metadata.TableInfoHelper", FULL_ACCESS);
        registerTypeQuietly(hints, "com.baomidou.mybatisplus.core.metadata.TableInfo", FULL_ACCESS);
        registerTypeQuietly(hints, "com.baomidou.mybatisplus.core.MybatisConfiguration", FULL_ACCESS);
        registerTypeQuietly(hints, "com.baomidou.mybatisplus.core.config.GlobalConfig", FULL_ACCESS);
        registerTypeQuietly(hints, "com.baomidou.mybatisplus.core.config.GlobalConfig$DbConfig", FULL_ACCESS);
        registerTypeQuietly(hints, "com.baomidou.mybatisplus.core.incrementer.DefaultIdentifierGenerator", FULL_ACCESS);

        // ---- MyBatis 语言驱动（LanguageDriverRegistry 反射实例化）----
        // Configuration 构造器通过 LanguageDriverRegistry.setDefaultDriverClass() 反射创建 XMLLanguageDriver
        registerTypeQuietly(hints, "org.apache.ibatis.scripting.xmltags.XMLLanguageDriver", FULL_ACCESS);
        registerTypeQuietly(hints, "org.apache.ibatis.scripting.defaults.RawLanguageDriver", FULL_ACCESS);
        registerTypeQuietly(hints, "org.apache.ibatis.scripting.xmltags.XMLScriptBuilder", FULL_ACCESS);
        registerTypeQuietly(hints, "org.apache.ibatis.scripting.xmltags.DynamicSqlSource", FULL_ACCESS);
        registerTypeQuietly(hints, "org.apache.ibatis.scripting.xmltags.TextSqlNode", FULL_ACCESS);
        registerTypeQuietly(hints, "org.apache.ibatis.scripting.xmltags.IfSqlNode", FULL_ACCESS);
        registerTypeQuietly(hints, "org.apache.ibatis.scripting.xmltags.ForEachSqlNode", FULL_ACCESS);
        registerTypeQuietly(hints, "org.apache.ibatis.scripting.xmltags.ChooseSqlNode", FULL_ACCESS);
        registerTypeQuietly(hints, "org.apache.ibatis.scripting.xmltags.MixedSqlNode", FULL_ACCESS);
        registerTypeQuietly(hints, "org.apache.ibatis.scripting.xmltags.WhereSqlNode", FULL_ACCESS);
        registerTypeQuietly(hints, "org.apache.ibatis.scripting.xmltags.SetSqlNode", FULL_ACCESS);
        registerTypeQuietly(hints, "org.apache.ibatis.scripting.xmltags.TrimSqlNode", FULL_ACCESS);
        registerTypeQuietly(hints, "org.apache.ibatis.scripting.xmltags.OgnlCache", FULL_ACCESS);
        registerTypeQuietly(hints, "org.apache.ibatis.scripting.LanguageDriverRegistry", FULL_ACCESS);

        // ---- MyBatis SQL 执行引擎 ----
        registerTypeQuietly(hints, "org.apache.ibatis.executor.SimpleExecutor", FULL_ACCESS);
        registerTypeQuietly(hints, "org.apache.ibatis.executor.statement.SimpleStatementHandler", FULL_ACCESS);
        registerTypeQuietly(hints, "org.apache.ibatis.executor.statement.PreparedStatementHandler", FULL_ACCESS);
        registerTypeQuietly(hints, "org.apache.ibatis.executor.resultset.DefaultResultSetHandler", FULL_ACCESS);
        registerTypeQuietly(hints, "org.apache.ibatis.executor.parameter.DefaultParameterHandler", FULL_ACCESS);
        registerTypeQuietly(hints, "org.apache.ibatis.scripting.xmltags.DynamicSqlSource", FULL_ACCESS);
        registerTypeQuietly(hints, "org.apache.ibatis.scripting.defaults.RawSqlSource", FULL_ACCESS);
        registerTypeQuietly(hints, "org.apache.ibatis.scripting.defaults.DefaultParameterHandler", FULL_ACCESS);

        // ---- MyBatis Spring 集成 ----
        registerTypeQuietly(hints, "org.mybatis.spring.SqlSessionTemplate", FULL_ACCESS);
        registerTypeQuietly(hints, "org.mybatis.spring.mapper.MapperFactoryBean", FULL_ACCESS);

        // ---- MyBatis Javassist 延迟加载代理（relocated 到 org.apache.ibatis.javassist）----
        // MyBatis Configuration 构造器无条件初始化 JavassistProxyFactory，
        // 即使未启用 lazyLoadingEnabled 也会触发类加载，必须注册反射提示
        registerTypeQuietly(hints, "org.apache.ibatis.executor.loader.javassist.JavassistProxyFactory", FULL_ACCESS);
        registerTypeQuietly(hints, "org.apache.ibatis.executor.loader.javassist.JavassistSerialStateHolder", FULL_ACCESS);
        registerTypeQuietly(hints, "org.apache.ibatis.javassist.util.proxy.ProxyFactory", FULL_ACCESS);
        registerTypeQuietly(hints, "org.apache.ibatis.javassist.util.proxy.ProxyObject", FULL_ACCESS);
        registerTypeQuietly(hints, "org.apache.ibatis.javassist.util.proxy.RuntimeSupport", FULL_ACCESS);
        registerTypeQuietly(hints, "org.apache.ibatis.javassist.util.proxy.SerializedProxy", FULL_ACCESS);
        registerTypeQuietly(hints, "org.apache.ibatis.javassist.util.proxy.MethodHandler", FULL_ACCESS);
        registerTypeQuietly(hints, "org.apache.ibatis.javassist.util.proxy.Proxy", FULL_ACCESS);
        registerTypeQuietly(hints, "org.apache.ibatis.javassist.ClassPool", FULL_ACCESS);
        registerTypeQuietly(hints, "org.apache.ibatis.javassist.CtClass", FULL_ACCESS);
        registerTypeQuietly(hints, "org.apache.ibatis.javassist.CtField", FULL_ACCESS);
        registerTypeQuietly(hints, "org.apache.ibatis.javassist.CtMethod", FULL_ACCESS);
        registerTypeQuietly(hints, "org.apache.ibatis.javassist.CtConstructor", FULL_ACCESS);

        // ---- MyBatis-Plus 扩展 ----
        registerTypeQuietly(hints, "com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor", FULL_ACCESS);
        registerTypeQuietly(hints, "com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor", FULL_ACCESS);
        registerTypeQuietly(hints, "com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler", FULL_ACCESS);
    }

    /**
     * 注册 MyBatis Mapper XML 和 Flyway 迁移脚本等资源提示。
     */
    private void registerMyBatisResources(RuntimeHints hints) {
        hints.resources().registerPattern("mapper/**/*.xml");
        hints.resources().registerPattern("db/migration/**/*.sql");
        hints.resources().registerPattern("application*.yml");
        hints.resources().registerPattern("application*.yaml");
        hints.resources().registerPattern("application*.properties");
        hints.resources().registerPattern("logback-spring.xml");
    }

    /**
     * 安静地注册类型反射提示，类不存在时跳过（兼容不同版本依赖）。
     */
    private void registerTypeQuietly(RuntimeHints hints, String className, MemberCategory... categories) {
        try {
            Class<?> clazz = Class.forName(className);
            hints.reflection().registerType(clazz, categories);
        } catch (ClassNotFoundException ignored) {
            // 类不存在于当前依赖版本，跳过
        }
    }
}
