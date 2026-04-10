package com.nfu.jasmine.cus.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * <p>
 * 花卉主数据
 * </p>
 *
 * @author jipzeongit
 * @since 2023-07-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("flower")
public class Flower implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private String name;

    private String unit;

    @TableField("sale_price")
    private BigDecimal salePrice;

    @TableField("cost_price")
    private BigDecimal costPrice;

    @TableField("safe_stock")
    private Integer safeStock;

    @TableField("current_stock")
    private Integer currentStock;

    private Integer status;

    private Integer deleted;
}
