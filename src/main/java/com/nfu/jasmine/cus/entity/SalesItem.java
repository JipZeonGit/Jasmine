package com.nfu.jasmine.cus.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * <p>
 * 销售明细
 * </p>
 */
@Data
@TableName("sales_item")
public class SalesItem implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("sales_id")
    private Integer salesId;

    @TableField("flower_id")
    private Integer flowerId;

    private Integer quantity;

    @TableField("unit_price")
    private BigDecimal unitPrice;

    @TableField("unit_cost")
    private BigDecimal unitCost;

    private BigDecimal amount;

    @TableField("cost_amount")
    private BigDecimal costAmount;

    private Integer deleted;
}
