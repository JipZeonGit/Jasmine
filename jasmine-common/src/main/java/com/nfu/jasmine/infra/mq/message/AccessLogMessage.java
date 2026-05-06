package com.nfu.jasmine.infra.mq.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 访问日志消息体，由 {@code RequestTraceFilter} 在每次请求结束后发布。
 * 消费端通过 {@code AccessLogAuditListener} 异步落地审计日志。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccessLogMessage implements Serializable {
    private String traceId;
    private String requestId;
    private Integer userId;
    private String username;
    private String method;
    private String uri;
    private Integer status;
    private Long durationMs;
    private String clientIp;
    private Date occurredAt;
}