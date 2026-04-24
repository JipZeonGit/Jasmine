package com.nfu.jasmine.infra.mq.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nfu.jasmine.infra.mq.JasmineMqConstants;
import com.nfu.jasmine.infra.outbox.model.entity.EventOutbox;
import com.nfu.jasmine.infra.outbox.model.enums.OutboxStatus;
import com.nfu.jasmine.infra.outbox.persistence.mapper.EventOutboxMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.interceptor.RetryOperationsInterceptor;
import org.springframework.retry.policy.SimpleRetryPolicy;

import java.util.HashMap;
import java.util.Map;

/**
 * RabbitMQ 拓扑与基础设施配置。
 * <p>
 * 通过 {@code app.mq.enabled=true} 开关控制是否激活，未开启时整个 MQ 层不注册任何 Bean。
 * 职责包括：声明交换机/队列/绑定关系、配置 JSON 消息转换器、统一重试策略和死信路由。
 */
@Configuration
@EnableRabbit
@ConditionalOnProperty(name = "app.mq.enabled", havingValue = "true")
public class RabbitMqTopologyConfig {
    private static final Logger log = LoggerFactory.getLogger(RabbitMqTopologyConfig.class);

    @Value("${app.mq.dead-letter-enabled:true}")
    private boolean deadLetterEnabled;

    @Value("${app.mq.retry.max-attempts:3}")
    private int retryMaxAttempts;

    @Value("${app.mq.retry.initial-interval-ms:1000}")
    private long retryInitialIntervalMs;

    @Value("${app.mq.retry.multiplier:2.0}")
    private double retryMultiplier;

    @Value("${app.mq.retry.max-interval-ms:10000}")
    private long retryMaxIntervalMs;

    @Bean
    public MessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
        // 统一使用 JSON 消息体，避免后面不同消息对象各自处理序列化。
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter rabbitMessageConverter,
                                         ObjectProvider<EventOutboxMapper> eventOutboxMapperProvider) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(rabbitMessageConverter);
        rabbitTemplate.setMandatory(true);
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (correlationData != null && correlationData.getId() != null) {
                try {
                    Long outboxId = Long.parseLong(correlationData.getId());
                    EventOutboxMapper outboxMapper = eventOutboxMapperProvider.getIfAvailable();
                    if (outboxMapper != null) {
                        if (ack) {
                            EventOutbox outbox = new EventOutbox();
                            outbox.setId(outboxId);
                            outbox.setStatus(OutboxStatus.SENT.name());
                            outbox.setSentAt(new java.util.Date());
                            outboxMapper.updateById(outbox);
                            log.debug("MQ 按期获批发送成功，反写回执 OutboxID={}", outboxId);
                        } else {
                            log.error("MQ 发布未获 broker 确认 outboxId={} cause={}", outboxId, cause);
                            // 未投到 Broker，不必干预，等此前 Relay 推迟的保护期结束会被重刷
                        }
                    }
                } catch (NumberFormatException e) {
                    // 非 Outbox 队列的常规使用不受影响
                    if (!ack) {
                        log.error("常规 MQ 发布失败 correlationId={} cause={}", correlationData.getId(), cause);
                    }
                }
            } else if (!ack) {
                log.error("未知 MQ 发布未获 broker 确认 cause={}", cause);
            }
        });
        rabbitTemplate.setReturnsCallback(returned -> log.error(
                "MQ 消息路由失败 exchange={} routingKey={} replyCode={} replyText={}",
                returned.getExchange(),
                returned.getRoutingKey(),
                returned.getReplyCode(),
                returned.getReplyText()
        ));
        return rabbitTemplate;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory,
                                                                              MessageConverter rabbitMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(rabbitMessageConverter);
        // 第一版不把失败消息直接打回原队列，避免消费异常时出现无休止重试。
        factory.setDefaultRequeueRejected(false);
        // 所有 listener 共用同一套重试规则，先把瞬时故障兜住，再决定是否进死信。
        factory.setAdviceChain(rabbitRetryInterceptor());
        return factory;
    }

    @Bean
    public RetryOperationsInterceptor rabbitRetryInterceptor() {
        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        // 这两类基本都属于"坏消息"，继续重试没有意义，直接拒绝更合适。
        retryableExceptions.put(AmqpRejectAndDontRequeueException.class, false);
        retryableExceptions.put(MessageConversionException.class, false);

        // 其余异常默认按"可恢复"处理，先给几次机会，避免一抖动就直接打进死信。
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(
                retryMaxAttempts,
                retryableExceptions,
                true,
                true
        );

        return RetryInterceptorBuilder.stateless()
                .retryPolicy(retryPolicy)
                // 这里统一做指数退避，避免消费者在故障期间高频空转刷日志。
                .backOffOptions(retryInitialIntervalMs, retryMultiplier, retryMaxIntervalMs)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();
    }

    @Bean
    public Declarables jasmineRabbitMqDeclarables() {
        TopicExchange appointmentExchange = new TopicExchange(JasmineMqConstants.APPOINTMENT_EVENT_EXCHANGE, true, false);
        TopicExchange auditExchange = new TopicExchange(JasmineMqConstants.AUDIT_EVENT_EXCHANGE, true, false);
        TopicExchange tradeExchange = new TopicExchange(JasmineMqConstants.TRADE_EVENT_EXCHANGE, true, false);
        TopicExchange deadLetterExchange = new TopicExchange(JasmineMqConstants.DEAD_LETTER_EXCHANGE, true, false);

        Queue appointmentQueue = buildBusinessQueue(JasmineMqConstants.APPOINTMENT_NOTIFICATION_QUEUE,
                JasmineMqConstants.DEAD_LETTER_EXCHANGE,
                "dead-letter.appointment.notification");
        Queue accessLogQueue = buildBusinessQueue(JasmineMqConstants.ACCESS_LOG_QUEUE,
                JasmineMqConstants.DEAD_LETTER_EXCHANGE,
                "dead-letter.audit.access-log");
        Queue salesEventQueue = buildBusinessQueue(JasmineMqConstants.SALES_EVENT_QUEUE,
                JasmineMqConstants.DEAD_LETTER_EXCHANGE,
                "dead-letter.sales.event-log");
        Queue inventoryEventQueue = buildBusinessQueue(JasmineMqConstants.INVENTORY_EVENT_QUEUE,
                JasmineMqConstants.DEAD_LETTER_EXCHANGE,
                "dead-letter.inventory.event-log");
        Queue deadLetterQueue = QueueBuilder.durable(JasmineMqConstants.DEAD_LETTER_QUEUE).build();

        // 延时驻留队列：无消费者监听，消息靠 per-message TTL 过期后，由死信路由弹射至提醒队列
        Queue appointmentDelayQueue = QueueBuilder.durable(JasmineMqConstants.APPOINTMENT_DELAY_QUEUE)
                .deadLetterExchange(JasmineMqConstants.APPOINTMENT_EVENT_EXCHANGE)
                .deadLetterRoutingKey(JasmineMqConstants.APPOINTMENT_REMINDER_ROUTING_KEY)
                .build();
        // 最终唤醒队列：消费者在此监听，收到消息后生成站内信
        Queue appointmentReminderQueue = buildBusinessQueue(JasmineMqConstants.APPOINTMENT_REMINDER_QUEUE,
                JasmineMqConstants.DEAD_LETTER_EXCHANGE,
                "dead-letter.appointment.reminder");

        Binding appointmentBinding = BindingBuilder.bind(appointmentQueue)
                .to(appointmentExchange)
                .with(JasmineMqConstants.APPOINTMENT_CREATED_ROUTING_KEY);
        // 延时队列绑定：Outbox Relay 把延时消息投到这条路由上
        Binding appointmentDelayBinding = BindingBuilder.bind(appointmentDelayQueue)
                .to(appointmentExchange)
                .with(JasmineMqConstants.APPOINTMENT_DELAY_ROUTING_KEY);
        // 唤醒队列绑定：死信弹射过来的消息落到这里
        Binding appointmentReminderBinding = BindingBuilder.bind(appointmentReminderQueue)
                .to(appointmentExchange)
                .with(JasmineMqConstants.APPOINTMENT_REMINDER_ROUTING_KEY);
        Binding accessLogBinding = BindingBuilder.bind(accessLogQueue)
                .to(auditExchange)
                .with(JasmineMqConstants.ACCESS_LOG_ROUTING_KEY);
        Binding salesEventBinding = BindingBuilder.bind(salesEventQueue)
                .to(tradeExchange)
                .with(JasmineMqConstants.SALES_CREATED_ROUTING_KEY);
        Binding inventoryEventBinding = BindingBuilder.bind(inventoryEventQueue)
                .to(tradeExchange)
                .with(JasmineMqConstants.INVENTORY_CHANGED_ROUTING_KEY);
        Binding deadLetterBinding = BindingBuilder.bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with(JasmineMqConstants.DEAD_LETTER_ROUTING_KEY);

        return new Declarables(
                appointmentExchange, auditExchange, tradeExchange, deadLetterExchange,
                appointmentQueue, appointmentDelayQueue, appointmentReminderQueue,
                accessLogQueue, salesEventQueue, inventoryEventQueue, deadLetterQueue,
                appointmentBinding, appointmentDelayBinding, appointmentReminderBinding,
                accessLogBinding, salesEventBinding, inventoryEventBinding, deadLetterBinding
        );
    }

    private Queue buildBusinessQueue(String queueName, String deadLetterExchange, String deadLetterRoutingKey) {
        QueueBuilder builder = QueueBuilder.durable(queueName);
        if (deadLetterEnabled) {
            // 死信能力先统一挂到业务队列上，后面补重试和排障时不需要再回头改拓扑。
            builder = builder.deadLetterExchange(deadLetterExchange).deadLetterRoutingKey(deadLetterRoutingKey);
        }
        return builder.build();
    }
}
