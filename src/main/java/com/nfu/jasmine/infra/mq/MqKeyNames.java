package com.nfu.jasmine.infra.mq;

/**
 * MQ 消费端幂等键生成工具。
 * <p>
 * 所有键共用前缀 {@code jasmine:mq:idempotent:}，后接 {事件类型}:{业务主键}，
 * 确保不同事件之间不会出现键冲突。新增事件时在这里追加对应方法即可。
 */
public final class MqKeyNames {
    private MqKeyNames() {
    }

    private static final String MQ_IDEMPOTENT_PREFIX = "jasmine:mq:idempotent:";

    public static String appointmentNotification(Integer appointmentId) {
        return MQ_IDEMPOTENT_PREFIX + "appointment-notification:" + appointmentId;
    }

    public static String appointmentReminder(Integer appointmentId, long time) {
        return MQ_IDEMPOTENT_PREFIX + "appointment-reminder:" + appointmentId + ":" + time;
    }

    public static String accessLog(String requestId) {
        return MQ_IDEMPOTENT_PREFIX + "access-log:" + requestId;
    }

    public static String salesCreated(Integer salesId) {
        return MQ_IDEMPOTENT_PREFIX + "sales-created:" + salesId;
    }

    public static String inventoryChanged(Integer inventoryId) {
        return MQ_IDEMPOTENT_PREFIX + "inventory-changed:" + inventoryId;
    }
}
