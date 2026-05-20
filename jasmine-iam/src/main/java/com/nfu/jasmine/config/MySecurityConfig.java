package com.nfu.jasmine.config;

import com.nfu.jasmine.infra.security.filter.JwtAuthenticationFilter;
import com.nfu.jasmine.infra.web.filter.RequestTraceFilter;
import com.nfu.jasmine.infra.security.handler.JwtAccessDeniedHandler;
import com.nfu.jasmine.infra.security.handler.JwtAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class MySecurityConfig {
    @Autowired
    private RequestTraceFilter requestTraceFilter;
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    @Autowired
    private JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                // 微服务架构下 CORS 由 Gateway 统一处理，下游服务不再需要
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/user/login",
                                "/user/refresh",
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/info",
                                "/actuator/prometheus",
                                "/error",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/swagger-resources/**"
                        ).permitAll()
                        .requestMatchers(
                                "/user/info",
                                "/user/logout",
                                "/user/changePassword",
                                "/site-message/**"
                        ).authenticated()
                        .requestMatchers(
                                "/user/**",
                                "/role/**",
                                "/menu/**",
                                "/sys/**"
                        ).hasRole("admin")
                        .requestMatchers(
                                "/vip/**",
                                "/appointment/**"
                        ).hasAnyRole("admin", "Boss")
                        .requestMatchers(
                                "/flower/**",
                                "/sales/**",
                                "/inventory/**",
                                "/inventory-alert/**"
                        ).hasAnyRole("admin", "Boss", "clerk")
                        .anyRequest().denyAll()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                )
                .addFilterBefore(requestTraceFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(jwtAuthenticationFilter, RequestTraceFilter.class);
        return http.build();
    }
}
