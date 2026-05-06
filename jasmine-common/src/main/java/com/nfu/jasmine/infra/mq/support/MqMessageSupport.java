package com.nfu.jasmine.infra.mq.support;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;

/**
 * MQ 消息前置校验工具。
 * <p>
 * 校验失败时抛出 {@link org.springframework.amqp.AmqpRejectAndDontRequeueException}，
 * 让坏消息直接进死信而不是反复重试，避免“毒消息”堵塞业务队列。
 */
public final class MqMessageSupport {
    private MqMessageSupport() {
    }

    public static void rejectIfBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new AmqpRejectAndDontRequeueException("MQ 消息缺少必要字段：" + fieldName);
        }
    }

    public static void rejectIfNull(Object value, String fieldName) {
        if (value == null) {
            throw new AmqpRejectAndDontRequeueException("MQ 消息缺少必要字段：" + fieldName);
        }
    }
}
