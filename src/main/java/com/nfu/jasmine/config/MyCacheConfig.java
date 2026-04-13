package com.nfu.jasmine.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import com.nfu.jasmine.infra.cache.CacheNames;

@Configuration
@Profile("!test")
@EnableCaching
@ConditionalOnProperty(name = "app.cache.type", havingValue = "memory")
public class MyCacheConfig {
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                CacheNames.USER,
                CacheNames.MENU_LIST,
                CacheNames.ROLE_LIST,
                CacheNames.FLOWER_LIST,
                CacheNames.FLOWER_DETAIL
        );
    }
}
