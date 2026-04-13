package com.nfu.jasmine.infra.mq.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

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
