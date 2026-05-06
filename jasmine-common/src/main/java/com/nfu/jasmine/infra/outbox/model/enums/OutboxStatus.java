package com.nfu.jasmine.infra.outbox.model.enums;

/**
 * Outbox 消息状态枚举。
 */
public enum OutboxStatus {
    PENDING,
    SENT,
    FAILED
}
