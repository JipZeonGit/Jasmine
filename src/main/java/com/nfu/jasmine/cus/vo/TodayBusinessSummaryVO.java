package com.nfu.jasmine.cus.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TodayBusinessSummaryVO {
    private BigDecimal todaySalesAmount;
    private BigDecimal todayPurchaseCost;
    private BigDecimal todayGrossProfit;
    private BigDecimal todayNetCashflow;
    private Integer todaySalesOrderCount;
    private Integer todayPurchaseCount;
}
