package com.nfu.jasmine.infra.mq.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

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