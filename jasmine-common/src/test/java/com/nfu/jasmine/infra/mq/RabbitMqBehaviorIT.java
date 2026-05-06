package com.nfu.jasmine.infra.mq;

import com.nfu.jasmine.config.AbstractIntegrationTest;
import com.nfu.jasmine.infra.mq.message.SalesCreatedMessage;
import com.nfu.jasmine.infra.mq.publisher.MqMessagePublisher;
import com.nfu.jasmine.infra.mq.support.MqIdempotencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * RabbitMQ 行为集成测试。
 * <p>
 * 覆盖场景：重试成功、重试耗尽进死信、幂等去重、坏消息拒绝、事务提交后发布、事务回滚不发布。
 * 通过内部 {@code RetryProbeListener} 和 {@code RetryProbeState} 配合验证 MQ 各项行为是否符合预期。
 */
@Import(RabbitMqBehaviorIT.TestRabbitMqConfig.class)
class RabbitMqBehaviorIT extends AbstractIntegrationTest {

    private static final String TEST_EXCHANGE = "jasmine.test.behavior.event";
    private static final String TEST_QUEUE = "jasmine.test.behavior";
    private static final String TEST_ROUTING_KEY = "test.behavior";
    private static final String TEST_DEAD_LETTER_ROUTING_KEY = "dead-letter.test.behavior";
    private static final String TEST_IDEMPOTENT_KEY_PREFIX = "jasmine:mq:idempotent:test-behavior:";

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private MqMessagePublisher mqMessagePublisher;

    @Autowired
    private RetryProbeState retryProbeState;

    @BeforeEach
    void setUp() {
        retryProbeState.reset();
        // 每个用例都从空队列开始，避免上一个用例残留消息影响断言。
        purgeQueue(JasmineMqConstants.SALES_EVENT_QUEUE);
        purgeQueue(JasmineMqConstants.DEAD_LETTER_QUEUE);
        purgeQueue(TEST_QUEUE);
    }

    @Test
    void retryableFailureShouldRetryAndEventuallySucceed() {
        String key = "retry-success";

        rabbitTemplate.convertAndSend(TEST_EXCHANGE, TEST_ROUTING_KEY,
                new RetryProbeMessage(key, ProbeMode.RETRY_THEN_SUCCESS.name()));

        waitUntil("等待测试 listener 重试成功", Duration.ofSeconds(5), () ->
                retryProbeState.attempts(key) == 3 && retryProbeState.processed(key) == 1);

        assertThat(messageCount(TEST_QUEUE)).isZero();
        assertThat(messageCount(JasmineMqConstants.DEAD_LETTER_QUEUE)).isZero();
    }

    @Test
    void retryExhaustedMessageShouldGoToDeadLetterQueue() {
        String key = "retry-exhausted";

        rabbitTemplate.convertAndSend(TEST_EXCHANGE, TEST_ROUTING_KEY,
                new RetryProbeMessage(key, ProbeMode.ALWAYS_FAIL.name()));

        waitUntil("等待测试 listener 重试耗尽并进入死信", Duration.ofSeconds(5), () ->
                retryProbeState.attempts(key) == 3 && messageCount(JasmineMqConstants.DEAD_LETTER_QUEUE) == 1);

        Message deadLetter = rabbitTemplate.receive(JasmineMqConstants.DEAD_LETTER_QUEUE);
        assertThat(deadLetter).isNotNull();
        assertThat(new String(deadLetter.getBody(), StandardCharsets.UTF_8)).contains(key);
        assertThat(deadLetter.getMessageProperties().getHeaders()).containsKey("x-death");
        assertThat(messageCount(TEST_QUEUE)).isZero();
    }

    @Test
    void duplicateMessageShouldBeSkippedByIdempotency() {
        String key = "duplicate-check";
        String redisKey = TEST_IDEMPOTENT_KEY_PREFIX + key;
        stringRedisTemplate.delete(redisKey);

        RetryProbeMessage message = new RetryProbeMessage(key, ProbeMode.IDEMPOTENT.name());
        rabbitTemplate.convertAndSend(TEST_EXCHANGE, TEST_ROUTING_KEY, message);
        rabbitTemplate.convertAndSend(TEST_EXCHANGE, TEST_ROUTING_KEY, message);

        waitUntil("等待幂等 listener 完成首条消费并跳过重复消息", Duration.ofSeconds(5), () ->
                retryProbeState.processed(key) == 1 && retryProbeState.duplicates(key) == 1);

        assertThat(stringRedisTemplate.hasKey(redisKey)).isTrue();
        assertThat(messageCount(TEST_QUEUE)).isZero();
        assertThat(messageCount(JasmineMqConstants.DEAD_LETTER_QUEUE)).isZero();
    }

    @Test
    void invalidSalesMessageShouldGoToDeadLetterQueue() {
        rabbitTemplate.convertAndSend(JasmineMqConstants.TRADE_EVENT_EXCHANGE, JasmineMqConstants.SALES_CREATED_ROUTING_KEY,
                new SalesCreatedMessage(null, "DLQ-ORDER", 1, 1, 1, new BigDecimal("88.00"), new Date(), new Date()));

        waitUntil("等待坏消息进入统一死信队列", Duration.ofSeconds(5),
                () -> messageCount(JasmineMqConstants.DEAD_LETTER_QUEUE) == 1);

        Message deadLetter = rabbitTemplate.receive(JasmineMqConstants.DEAD_LETTER_QUEUE);
        assertThat(deadLetter).isNotNull();
        assertThat(new String(deadLetter.getBody(), StandardCharsets.UTF_8)).contains("DLQ-ORDER");
        assertThat(deadLetter.getMessageProperties().getHeaders()).containsKey("x-death");
        assertThat(messageCount(JasmineMqConstants.SALES_EVENT_QUEUE)).isZero();
    }

    @Test
    void committedTransactionShouldPublishSalesEventAfterCommit() {
        int salesId = 990001;
        String redisKey = MqKeyNames.salesCreated(salesId);
        stringRedisTemplate.delete(redisKey);

        // 这里故意放进事务里，验证 afterCommit 注册的发布逻辑真的会在提交后触发。
        transactionTemplate.executeWithoutResult(status -> mqMessagePublisher.publishSalesCreatedAfterCommit(
                new SalesCreatedMessage(salesId, "COMMIT-ORDER", 1, 1, 1, new BigDecimal("99.00"), new Date(), new Date())
        ));

        waitUntil("等待事务提交后销售事件被消费", Duration.ofSeconds(5),
                () -> Boolean.TRUE.equals(stringRedisTemplate.hasKey(redisKey)));

        assertThat(messageCount(JasmineMqConstants.SALES_EVENT_QUEUE)).isZero();
        assertThat(messageCount(JasmineMqConstants.DEAD_LETTER_QUEUE)).isZero();
    }

    @Test
    void rolledBackTransactionShouldNotPublishSalesEvent() throws InterruptedException {
        int salesId = 990002;
        String redisKey = MqKeyNames.salesCreated(salesId);
        stringRedisTemplate.delete(redisKey);

        transactionTemplate.executeWithoutResult(status -> {
            // 先注册“提交后发布”，再显式回滚，确认不会产生幽灵消息。
            mqMessagePublisher.publishSalesCreatedAfterCommit(
                    new SalesCreatedMessage(salesId, "ROLLBACK-ORDER", 1, 1, 1, new BigDecimal("66.00"), new Date(), new Date())
            );
            status.setRollbackOnly();
        });

        Thread.sleep(300);

        assertThat(stringRedisTemplate.hasKey(redisKey)).isFalse();
        assertThat(messageCount(JasmineMqConstants.SALES_EVENT_QUEUE)).isZero();
        assertThat(messageCount(JasmineMqConstants.DEAD_LETTER_QUEUE)).isZero();
    }

    private void purgeQueue(String queueName) {
        rabbitTemplate.execute(channel -> {
            channel.queuePurge(queueName);
            return null;
        });
    }

    private long messageCount(String queueName) {
        Long count = rabbitTemplate.execute(channel -> channel.messageCount(queueName));
        return count == null ? 0L : count;
    }

    private void waitUntil(String description, Duration timeout, Supplier<Boolean> condition) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (condition.get()) {
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                fail(description + " 时等待被中断");
            }
        }
        fail(description);
    }

    // 测试用探针模式：控制 listener 在不同场景下的行为。
    enum ProbeMode {
        RETRY_THEN_SUCCESS,
        ALWAYS_FAIL,
        IDEMPOTENT
    }

    // 测试专用消息体，携带业务 key 和探针模式来驱动不同的测试分支。
    static class RetryProbeMessage {
        private String key;
        private String mode;

        public RetryProbeMessage() {
        }

        public RetryProbeMessage(String key, String mode) {
            this.key = key;
            this.mode = mode;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }
    }

    // 记录测试 listener 的执行状态（尝试次数、成功消费数、重复跳过数），供测试断言使用。
    static class RetryProbeState {
        private final Map<String, AtomicInteger> attempts = new ConcurrentHashMap<>();
        private final Map<String, AtomicInteger> processed = new ConcurrentHashMap<>();
        private final Map<String, AtomicInteger> duplicates = new ConcurrentHashMap<>();

        void recordAttempt(String key) {
            attempts.computeIfAbsent(key, ignored -> new AtomicInteger()).incrementAndGet();
        }

        void recordProcessed(String key) {
            processed.computeIfAbsent(key, ignored -> new AtomicInteger()).incrementAndGet();
        }

        void recordDuplicate(String key) {
            duplicates.computeIfAbsent(key, ignored -> new AtomicInteger()).incrementAndGet();
        }

        int attempts(String key) {
            return attempts.getOrDefault(key, new AtomicInteger()).get();
        }

        int processed(String key) {
            return processed.getOrDefault(key, new AtomicInteger()).get();
        }

        int duplicates(String key) {
            return duplicates.getOrDefault(key, new AtomicInteger()).get();
        }

        void reset() {
            attempts.clear();
            processed.clear();
            duplicates.clear();
        }
    }

    // 测试专用 listener，根据 ProbeMode 分别模拟重试成功、持续失败、幂等去重等场景。
    static class RetryProbeListener {
        private final MqIdempotencyService mqIdempotencyService;
        private final RetryProbeState retryProbeState;

        RetryProbeListener(MqIdempotencyService mqIdempotencyService, RetryProbeState retryProbeState) {
            this.mqIdempotencyService = mqIdempotencyService;
            this.retryProbeState = retryProbeState;
        }

        @RabbitListener(queues = TEST_QUEUE)
        public void handle(RetryProbeMessage message) {
            if (message == null || message.getKey() == null || message.getKey().isBlank()) {
                throw new IllegalArgumentException("测试消息缺少 key");
            }

            retryProbeState.recordAttempt(message.getKey());
            ProbeMode mode = ProbeMode.valueOf(message.getMode());
            if (mode == ProbeMode.RETRY_THEN_SUCCESS && retryProbeState.attempts(message.getKey()) < 3) {
                // 前两次故意失败，用来证明容器上的重试 advice 已经生效。
                throw new IllegalStateException("模拟瞬时异常，触发重试");
            }
            if (mode == ProbeMode.ALWAYS_FAIL) {
                // 这类一直失败的消息，最终应该被 recoverer 丢进统一死信队列。
                throw new IllegalStateException("模拟持续异常，最终进入死信");
            }
            if (mode == ProbeMode.IDEMPOTENT) {
                String key = TEST_IDEMPOTENT_KEY_PREFIX + message.getKey();
                if (!mqIdempotencyService.markIfFirstConsume(key)) {
                    // 第二次命中同一个业务 key 时，只记一次“重复被跳过”。
                    retryProbeState.recordDuplicate(message.getKey());
                    return;
                }
            }
            retryProbeState.recordProcessed(message.getKey());
        }
    }

    // 测试配置：注册测试专用的交换机、队列绑定和 listener，与业务拓扑隔离。
    @TestConfiguration
    static class TestRabbitMqConfig {
        @Bean
        RetryProbeState retryProbeState() {
            return new RetryProbeState();
        }

        @Bean
        RetryProbeListener retryProbeListener(MqIdempotencyService mqIdempotencyService, RetryProbeState retryProbeState) {
            return new RetryProbeListener(mqIdempotencyService, retryProbeState);
        }

        @Bean
        Declarables testRabbitMqDeclarables() {
            TopicExchange exchange = new TopicExchange(TEST_EXCHANGE, true, false);
            Queue queue = QueueBuilder.durable(TEST_QUEUE)
                    .deadLetterExchange(JasmineMqConstants.DEAD_LETTER_EXCHANGE)
                    .deadLetterRoutingKey(TEST_DEAD_LETTER_ROUTING_KEY)
                    .build();
            Binding binding = BindingBuilder.bind(queue).to(exchange).with(TEST_ROUTING_KEY);
            return new Declarables(exchange, queue, binding);
        }
    }
}
