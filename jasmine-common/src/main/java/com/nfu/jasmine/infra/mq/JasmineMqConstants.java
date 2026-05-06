package com.nfu.jasmine.infra.mq;

/**
 * RabbitMQ 拓扑常量，按业务域分组。
 * <p>
 * 命名规则：交换机 = jasmine.{域}.event，队列 = jasmine.{域}.{用途}，路由键 = {域}.{动作}。
 * 新增事件时按同样规则追加，保持拓扑可预测。
 */
public final class JasmineMqConstants {
    private JasmineMqConstants() {
    }

    // ---- 预约域 ----
    public static final String APPOINTMENT_EVENT_EXCHANGE = "jasmine.appointment.event";
    public static final String APPOINTMENT_NOTIFICATION_QUEUE = "jasmine.appointment.notification";
    public static final String APPOINTMENT_CREATED_ROUTING_KEY = "appointment.created";
    // 延时提醒：死信驻留队列（无消费者，靠 per-message TTL 过期后弹射至 reminder 队列）
    public static final String APPOINTMENT_DELAY_QUEUE = "jasmine.appointment.delay";
    public static final String APPOINTMENT_DELAY_ROUTING_KEY = "appointment.delay";
    // 延时提醒：最终唤醒队列（消费者监听此队列生成站内信）
    public static final String APPOINTMENT_REMINDER_QUEUE = "jasmine.appointment.reminder";
    public static final String APPOINTMENT_REMINDER_ROUTING_KEY = "appointment.reminder";

    // ---- 审计域 ----
    public static final String AUDIT_EVENT_EXCHANGE = "jasmine.audit.event";
    public static final String ACCESS_LOG_QUEUE = "jasmine.audit.access-log";
    public static final String ACCESS_LOG_ROUTING_KEY = "audit.access-log";

    // ---- 交易域（销售 + 库存） ----
    public static final String TRADE_EVENT_EXCHANGE = "jasmine.trade.event";
    public static final String SALES_EVENT_QUEUE = "jasmine.sales.event-log";
    public static final String INVENTORY_EVENT_QUEUE = "jasmine.inventory.event-log";
    public static final String SALES_CREATED_ROUTING_KEY = "sales.created";
    public static final String INVENTORY_CHANGED_ROUTING_KEY = "inventory.changed";

    // ---- 统一死信 ----
    public static final String DEAD_LETTER_EXCHANGE = "jasmine.dlx";
    public static final String DEAD_LETTER_QUEUE = "jasmine.dlq";
    public static final String DEAD_LETTER_ROUTING_KEY = "dead-letter.#";
}
