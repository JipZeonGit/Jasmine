package com.nfu.jasmine;

import com.nfu.jasmine.config.AbstractIntegrationTest;
import com.nfu.jasmine.iam.model.entity.User;
import com.nfu.jasmine.iam.persistence.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JasmineApplicationIT extends AbstractIntegrationTest {

    @Autowired
    private UserMapper userMapper;

    @Test
    void baselineMigrationShouldSeedDefaultUsers() {
        List<User> users = userMapper.selectList(null);
        assertThat(users)
                .isNotEmpty()
                .extracting(User::getUsername)
                .contains("admin");
    }
}