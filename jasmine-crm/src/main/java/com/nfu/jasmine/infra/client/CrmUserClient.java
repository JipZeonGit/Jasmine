package com.nfu.jasmine.infra.client;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;

/**
 * IAM 服务远程调用客户端（CRM 专用）。
 * <p>
 * 基于 Spring 6 HTTP Interface + RestClient，通过 Spring Cloud LoadBalancer 实现服务发现。
 */
@HttpExchange(url = "/internal/user", contentType = "application/json")
public interface CrmUserClient {

    @GetExchange("/active-ids-by-roles")
    List<Integer> getActiveUserIdsByRoles(@RequestParam List<String> roleNames);
}
