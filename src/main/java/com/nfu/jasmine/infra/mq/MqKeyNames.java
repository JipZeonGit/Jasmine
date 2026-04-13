package com.nfu.jasmine.infra.mq;

public final class MqKeyNames {
    private MqKeyNames() {
    }

    private static final String MQ_IDEMPOTENT_PREFIX = "jasmine:mq:idempotent:";

    public static String appointmentNotification(Integer appointmentId) {
        return MQ_IDEMPOTENT_PREFIX + "appointment-notification:" + appointmentId;
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
