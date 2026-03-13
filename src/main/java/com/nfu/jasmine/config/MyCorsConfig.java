package com.nfu.jasmine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.beans.factory.annotation.Value;
import java.util.List;

@Configuration
public class MyCorsConfig {
    
    @Value("${app.cors.allowed-origins:http://localhost:8888,http://localhost}")
    private List<String> allowedOrigins;
    //全局跨域请求配置
    @Bean
    public CorsFilter corsFilter(){
        //添加CORS配置信息
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        // 动态读取允许访问的域（白名单安全机制）
        if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
            for (String origin : allowedOrigins) {
                corsConfiguration.addAllowedOrigin(origin);
            }
        }
        //允许发送cookie信息
        corsConfiguration.setAllowCredentials(true);
        //允许的请求方式
        corsConfiguration.addAllowedMethod("*");
        //允许的请求头信息
        corsConfiguration.addAllowedHeader("*");
        //拦截请求
        UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = new UrlBasedCorsConfigurationSource();
        urlBasedCorsConfigurationSource.registerCorsConfiguration("/**",corsConfiguration);
        //返回新的CorsFilter
        return new CorsFilter(urlBasedCorsConfigurationSource);
    }
}
