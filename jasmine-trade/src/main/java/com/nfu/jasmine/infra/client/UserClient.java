package com.nfu.jasmine.infra.client;

import com.nfu.jasmine.common.dto.internal.UserBasicDTO;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * IAM 服务远程调用客户端。
 * <p>
 * 基于 Spring 6 HTTP Interface + RestClient，通过 Spring Cloud LoadBalancer 实现服务发现。
 */
@HttpExchange(url = "/internal/user", contentType = "application/json")
public interface UserClient {

    @GetExchange("/{id}")
    UserBasicDTO getUserById(@PathVariable Integer id);
}
