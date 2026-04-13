package com.nfu.jasmine.infra.mq;

public final class JasmineMqConstants {
    private JasmineMqConstants() {
    }

    public static final String APPOINTMENT_EVENT_EXCHANGE = "jasmine.appointment.event";
    public static final String APPOINTMENT_NOTIFICATION_QUEUE = "jasmine.appointment.notification";
    public static final String APPOINTMENT_CREATED_ROUTING_KEY = "appointment.created";

    public static final String AUDIT_EVENT_EXCHANGE = "jasmine.audit.event";
    public static final String ACCESS_LOG_QUEUE = "jasmine.audit.access-log";
    public static final String ACCESS_LOG_ROUTING_KEY = "audit.access-log";

    public static final String TRADE_EVENT_EXCHANGE = "jasmine.trade.event";
    public static final String SALES_EVENT_QUEUE = "jasmine.sales.event-log";
    public static final String INVENTORY_EVENT_QUEUE = "jasmine.inventory.event-log";
    public static final String SALES_CREATED_ROUTING_KEY = "sales.created";
    public static final String INVENTORY_CHANGED_ROUTING_KEY = "inventory.changed";

    public static final String DEAD_LETTER_EXCHANGE = "jasmine.dlx";
    public static final String DEAD_LETTER_QUEUE = "jasmine.dlq";
    public static final String DEAD_LETTER_ROUTING_KEY = "dead-letter.#";
}
