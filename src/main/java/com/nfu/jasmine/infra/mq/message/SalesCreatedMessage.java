package com.nfu.jasmine.infra.mq.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 销售单创建消息体，在销售事务提交后发布，供统计、审计等下游消费者使用。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalesCreatedMessage implements Serializable {
    private Integer salesId;
    private String orderNo;
    private Integer vipId;
    private Integer operatorId;
    private Integer itemCount;
    private BigDecimal totalAmount;
    private Date salesTime;
    private Date occurredAt;
}
