package com.nfu.jasmine.common.model;

/**
 * 登录用户信息提取接口。
 * <p>
 * 用于解耦 RequestTraceFilter / MQ 消息等基础设施对 iam.model.entity.User 的直接依赖。
 * 具体实现由各服务的 User 实体或 DTO 适配。
 */
public interface LoginUserInfo {
    Integer getId();
    String getUsername();
}
