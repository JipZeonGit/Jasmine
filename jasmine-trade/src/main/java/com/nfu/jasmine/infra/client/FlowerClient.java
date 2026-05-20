package com.nfu.jasmine.infra.client;

import com.nfu.jasmine.common.dto.internal.FlowerDTO;
import com.nfu.jasmine.common.dto.internal.StockAdjustRequest;
import com.nfu.jasmine.common.dto.internal.StockAdjustResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * 商品服务远程调用客户端。
 * <p>
 * 基于 Spring 6 HTTP Interface + RestClient，通过 Spring Cloud LoadBalancer 实现服务发现。
 */
@HttpExchange(url = "/internal/flower", contentType = "application/json")
public interface FlowerClient {

    @GetExchange("/{id}")
    FlowerDTO getFlowerById(@PathVariable Integer id);

    @PostExchange("/stock/adjust")
    StockAdjustResult adjustStock(@RequestBody StockAdjustRequest request);
}
