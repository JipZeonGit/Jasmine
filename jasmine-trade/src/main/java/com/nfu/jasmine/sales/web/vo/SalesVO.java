package com.nfu.jasmine.sales.web.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class SalesVO {
    private Integer id;
    private String orderNo;
    private Integer vipId;
    private String vipName;
    private String vipPhone;
    private BigDecimal totalAmount;
    private String remark;
    private Integer itemCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date date;

    private List<SalesItemVO> items;
}
