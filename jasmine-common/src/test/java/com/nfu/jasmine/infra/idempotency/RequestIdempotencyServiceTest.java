package com.nfu.jasmine.infra.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nfu.jasmine.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestIdempotencyServiceTest {

    @Test
    void shouldRejectDuplicateCreateRequestsWithinTtl() {
        RequestIdempotencyService service = new RequestIdempotencyService(null, new ObjectMapper());
        ReflectionTestUtils.setField(service, "ttlSeconds", 30L);
        AtomicInteger counter = new AtomicInteger();

        service.executeCreate("sales:create", 1, null, Map.of("flowerId", 1, "quantity", 10), counter::incrementAndGet);

        assertThatThrownBy(() -> service.executeCreate("sales:create", 1, null, Map.of("flowerId", 1, "quantity", 10), counter::incrementAndGet))
                .isInstanceOf(BusinessException.class)
                .hasMessage("请求重复提交，请稍后再试！");
        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    void shouldReleaseKeyWhenActionFails() {
        RequestIdempotencyService service = new RequestIdempotencyService(null, new ObjectMapper());
        ReflectionTestUtils.setField(service, "ttlSeconds", 30L);

        assertThatThrownBy(() -> service.executeCreate("inventory:create", 1, null, Map.of("flowerId", 2), () -> {
            throw new IllegalStateException("boom");
        })).isInstanceOf(IllegalStateException.class);

        AtomicInteger counter = new AtomicInteger();
        service.executeCreate("inventory:create", 1, null, Map.of("flowerId", 2), counter::incrementAndGet);
        assertThat(counter.get()).isEqualTo(1);
    }
}
