package com.nfu.jasmine.infra.mq.listener;

import com.nfu.jasmine.infra.mq.JasmineMqConstants;
import com.nfu.jasmine.infra.mq.MqKeyNames;
import com.nfu.jasmine.infra.mq.message.AccessLogMessage;
import com.nfu.jasmine.infra.mq.support.MqIdempotencyService;
import com.nfu.jasmine.infra.mq.support.MqMessageSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 访问日志审计消费者。
 * <p>
 * 将请求追踪过滤器异步发来的访问日志落地到独立的 ACCESS_LOG logger，
 * 并在 MDC 中还原 traceId / requestId 以保持日志链路完整。
 */
@Component
@ConditionalOnProperty(name = "app.mq.enabled", havingValue = "true")
public class AccessLogAuditListener {
    private static final Logger accessLogger = LoggerFactory.getLogger("ACCESS_LOG");

    private final MqIdempotencyService mqIdempotencyService;

    public AccessLogAuditListener(MqIdempotencyService mqIdempotencyService) {
        this.mqIdempotencyService = mqIdempotencyService;
    }

    @RabbitListener(queues = JasmineMqConstants.ACCESS_LOG_QUEUE)
    public void handle(AccessLogMessage message) {
        MqMessageSupport.rejectIfNull(message, "message");
        MqMessageSupport.rejectIfBlank(message.getRequestId(), "requestId");

        String key = MqKeyNames.accessLog(message.getRequestId());
        if (!mqIdempotencyService.markIfFirstConsume(key)) {
            accessLogger.info("跳过重复访问日志消息 requestId={}", message.getRequestId());
            return;
        }
        try {
            // 消费端把 traceId / requestId 再塞回 MDC，这样异步日志落地后依然能保持原请求链路信息。
            MDC.put("traceId", message.getTraceId());
            MDC.put("requestId", message.getRequestId());
            accessLogger.info("method={} uri={} status={} durationMs={} clientIp={} userId={} username={}",
                    message.getMethod(),
                    message.getUri(),
                    message.getStatus(),
                    message.getDurationMs(),
                    message.getClientIp(),
                    message.getUserId(),
                    message.getUsername());
        } finally {
            MDC.remove("traceId");
            MDC.remove("requestId");
        }
    }
}
