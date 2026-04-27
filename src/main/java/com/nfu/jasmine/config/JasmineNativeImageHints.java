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

    @Override
    public void registerHints(@NonNull RuntimeHints hints, ClassLoader classLoader) {
        registerEntities(hints);
        registerMqMessages(hints);
        registerEnums(hints);
        registerSecurityClasses(hints);
        registerMyBatisProxies(hints);
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
            hints.reflection().registerType(entity,
                    org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_METHODS,
                    org.springframework.aot.hint.MemberCategory.INVOKE_DECLARED_METHODS,
                    org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                    org.springframework.aot.hint.MemberCategory.DECLARED_FIELDS,
                    org.springframework.aot.hint.MemberCategory.PUBLIC_FIELDS);
        }
        // Result<T> 泛型容器被 Jackson / fastjson2 反射访问
        hints.reflection().registerType(Result.class,
                org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_METHODS,
                org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                org.springframework.aot.hint.MemberCategory.DECLARED_FIELDS);
        // JwtTokenClaims 被 JJWT 解析后反射构建
        hints.reflection().registerType(JwtTokenClaims.class,
                org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_METHODS,
                org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                org.springframework.aot.hint.MemberCategory.DECLARED_FIELDS);
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
            hints.reflection().registerType(message,
                    org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_METHODS,
                    org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                    org.springframework.aot.hint.MemberCategory.DECLARED_FIELDS);
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
                    org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_METHODS,
                    org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
        }
    }

    /**
     * 注册安全相关类的反射提示。
     * BCryptPasswordEncoder / JJWT 内部使用反射。
     */
    private void registerSecurityClasses(RuntimeHints hints) {
        // BCryptPasswordEncoder
        hints.reflection().registerType(
                org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder.class,
                org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_METHODS,
                org.springframework.aot.hint.MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
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
}
