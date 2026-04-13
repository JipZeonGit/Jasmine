package com.nfu.jasmine.infra.mq.support;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;

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
