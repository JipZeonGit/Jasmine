package com.nfu.jasmine.infra.mq.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nfu.jasmine.infra.mq.JasmineMqConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
@ConditionalOnProperty(name = "app.mq.enabled", havingValue = "true")
public class RabbitMqTopologyConfig {

    @Value("${app.mq.dead-letter-enabled:true}")
    private boolean deadLetterEnabled;

    @Bean
    public MessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
        // 统一使用 JSON 消息体，避免后面不同消息对象各自处理序列化。
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter rabbitMessageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(rabbitMessageConverter);
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
        return factory;
    }

    @Bean
    public Declarables jasmineRabbitMqDeclarables() {
        // 第二阶段先把销售和库存事件的“消息骨架”补上，后面再逐步接统计、预警和更复杂的下游。
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

        Binding appointmentBinding = BindingBuilder.bind(appointmentQueue)
                .to(appointmentExchange)
                .with(JasmineMqConstants.APPOINTMENT_CREATED_ROUTING_KEY);
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
                appointmentExchange,
                auditExchange,
                tradeExchange,
                deadLetterExchange,
                appointmentQueue,
                accessLogQueue,
                salesEventQueue,
                inventoryEventQueue,
                deadLetterQueue,
                appointmentBinding,
                accessLogBinding,
                salesEventBinding,
                inventoryEventBinding,
                deadLetterBinding
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
