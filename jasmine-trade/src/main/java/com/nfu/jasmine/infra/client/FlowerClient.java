package com.nfu.jasmine.infra.client;

import com.nfu.jasmine.common.dto.internal.FlowerDTO;
import com.nfu.jasmine.common.dto.internal.StockAdjustRequest;
import com.nfu.jasmine.common.dto.internal.StockAdjustResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

/**
 * 商品服务远程调用客户端。
 * <p>
 * 基于 Spring 6 HTTP Interface + RestClient，通过 Spring Cloud LoadBalancer 实现服务发现。
 * <p>
 * adjustStock 虽然声明返回 ResponseEntity，但 RestClient 默认对 4xx 直接抛
 * HttpClientErrorException，业务失败的 422 不会进入 ResponseEntity 分支。
 * 由 {@link RemoteProductStockFacade#doAdjustStock} 统一捕获并转回 BusinessException。
 */
@HttpExchange(url = "/internal/flower", contentType = "application/json")
public interface FlowerClient {

    @GetExchange("/{id}")
    FlowerDTO getFlowerById(@PathVariable Integer id);

    @PostExchange("/batch")
    List<FlowerDTO> getFlowersByIds(@RequestBody List<Integer> ids);

    @GetExchange("/ids-by-name")
    List<Integer> getFlowerIdsByName(@RequestParam String name);

    @PostExchange("/stock/adjust")
    ResponseEntity<StockAdjustResult> adjustStock(@RequestBody StockAdjustRequest request);
}
