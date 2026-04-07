package com.nfu.jasmine.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayBaselineScriptTest {

    @Test
    void baselineScriptShouldBeCleanedForFlyway() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/migration/V1__baseline.sql");

        assertThat(resource.exists()).isTrue();

        String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        assertThat(sql).contains("CREATE TABLE `user`");
        assertThat(sql).doesNotContain("CREATE DATABASE");
        assertThat(sql).doesNotContain("DROP TABLE IF EXISTS");
        assertThat(sql).doesNotContain("SET FOREIGN_KEY_CHECKS");
        assertThat(sql).doesNotContain("USE `jasmine`");
        assertThat(sql).doesNotContain("INSERT INTO `user_role` VALUES (2, 3, 2);");
        assertThat(sql).doesNotContain("INSERT INTO `user_role` VALUES (4, 5, 4);");
    }
}