package com.nfu.jasmine.config;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nfu.jasmine.appointment.persistence.mapper.AppointmentMapper;
import com.nfu.jasmine.flower.persistence.mapper.FlowerMapper;
import com.nfu.jasmine.iam.persistence.mapper.AuthRefreshTokenMapper;
import com.nfu.jasmine.iam.persistence.mapper.MenuMapper;
import com.nfu.jasmine.iam.persistence.mapper.RoleMapper;
import com.nfu.jasmine.iam.persistence.mapper.RoleMenuMapper;
import com.nfu.jasmine.iam.persistence.mapper.UserMapper;
import com.nfu.jasmine.iam.persistence.mapper.UserRoleMapper;
import com.nfu.jasmine.infra.notification.persistence.mapper.SiteMessageMapper;
import com.nfu.jasmine.infra.outbox.persistence.mapper.EventOutboxMapper;
import com.nfu.jasmine.inventory.alert.persistence.mapper.InventoryAlertMapper;
import com.nfu.jasmine.inventory.persistence.mapper.InventoryMapper;
import com.nfu.jasmine.sales.persistence.mapper.SalesItemMapper;
import com.nfu.jasmine.sales.persistence.mapper.SalesMapper;
import com.nfu.jasmine.vip.persistence.mapper.VipMapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Mapper 接口注册配置。
 * <p>
 * 使用 {@code @Bean} 方法逐个注册 {@link MapperFactoryBean}，
 * 而非 {@code @MapperScan} 扫描。原因是 mybatis-plus-spring-boot3-starter
 * 没有提供 Spring AOT 的 {@code BeanRegistrationAotProcessor}，
 * 导致 GraalVM Native Image 构建时 Spring AOT 为 MapperFactoryBean
 * 生成了错误的自动装配代码（尝试从容器中 autowire {@code Class<?>} 参数）。
 * <p>
 * 使用 {@code @Bean} 方法时，{@code Class<?>} 作为编译期常量直接传入构造器，
 * 不经过自动装配，从而绕过 AOT 代码生成缺陷。JVM 模式同样兼容。
 */
@Configuration
public class MapperConfig {

    private <T extends BaseMapper<?>> MapperFactoryBean<T> mapperFactoryBean(
            Class<T> mapperInterface, SqlSessionFactory sqlSessionFactory) {
        MapperFactoryBean<T> factory = new MapperFactoryBean<>(mapperInterface);
        factory.setSqlSessionFactory(sqlSessionFactory);
        return factory;
    }

    // ---- 业务模块 Mapper ----

    @Bean
    public MapperFactoryBean<FlowerMapper> flowerMapper(SqlSessionFactory sqlSessionFactory) {
        return mapperFactoryBean(FlowerMapper.class, sqlSessionFactory);
    }

    @Bean
    public MapperFactoryBean<SalesMapper> salesMapper(SqlSessionFactory sqlSessionFactory) {
        return mapperFactoryBean(SalesMapper.class, sqlSessionFactory);
    }

    @Bean
    public MapperFactoryBean<SalesItemMapper> salesItemMapper(SqlSessionFactory sqlSessionFactory) {
        return mapperFactoryBean(SalesItemMapper.class, sqlSessionFactory);
    }

    @Bean
    public MapperFactoryBean<InventoryMapper> inventoryMapper(SqlSessionFactory sqlSessionFactory) {
        return mapperFactoryBean(InventoryMapper.class, sqlSessionFactory);
    }

    @Bean
    public MapperFactoryBean<InventoryAlertMapper> inventoryAlertMapper(SqlSessionFactory sqlSessionFactory) {
        return mapperFactoryBean(InventoryAlertMapper.class, sqlSessionFactory);
    }

    @Bean
    public MapperFactoryBean<AppointmentMapper> appointmentMapper(SqlSessionFactory sqlSessionFactory) {
        return mapperFactoryBean(AppointmentMapper.class, sqlSessionFactory);
    }

    @Bean
    public MapperFactoryBean<VipMapper> vipMapper(SqlSessionFactory sqlSessionFactory) {
        return mapperFactoryBean(VipMapper.class, sqlSessionFactory);
    }

    // ---- IAM 模块 Mapper ----

    @Bean
    public MapperFactoryBean<UserMapper> userMapper(SqlSessionFactory sqlSessionFactory) {
        return mapperFactoryBean(UserMapper.class, sqlSessionFactory);
    }

    @Bean
    public MapperFactoryBean<RoleMapper> roleMapper(SqlSessionFactory sqlSessionFactory) {
        return mapperFactoryBean(RoleMapper.class, sqlSessionFactory);
    }

    @Bean
    public MapperFactoryBean<MenuMapper> menuMapper(SqlSessionFactory sqlSessionFactory) {
        return mapperFactoryBean(MenuMapper.class, sqlSessionFactory);
    }

    @Bean
    public MapperFactoryBean<UserRoleMapper> userRoleMapper(SqlSessionFactory sqlSessionFactory) {
        return mapperFactoryBean(UserRoleMapper.class, sqlSessionFactory);
    }

    @Bean
    public MapperFactoryBean<RoleMenuMapper> roleMenuMapper(SqlSessionFactory sqlSessionFactory) {
        return mapperFactoryBean(RoleMenuMapper.class, sqlSessionFactory);
    }

    @Bean
    public MapperFactoryBean<AuthRefreshTokenMapper> authRefreshTokenMapper(SqlSessionFactory sqlSessionFactory) {
        return mapperFactoryBean(AuthRefreshTokenMapper.class, sqlSessionFactory);
    }

    // ---- 基础设施模块 Mapper ----

    @Bean
    public MapperFactoryBean<EventOutboxMapper> eventOutboxMapper(SqlSessionFactory sqlSessionFactory) {
        return mapperFactoryBean(EventOutboxMapper.class, sqlSessionFactory);
    }

    @Bean
    public MapperFactoryBean<SiteMessageMapper> siteMessageMapper(SqlSessionFactory sqlSessionFactory) {
        return mapperFactoryBean(SiteMessageMapper.class, sqlSessionFactory);
    }
}
