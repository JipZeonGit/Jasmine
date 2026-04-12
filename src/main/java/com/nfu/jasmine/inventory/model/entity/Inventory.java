package com.nfu.jasmine.inventory.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * <p>
 * 库存动作流水
 * </p>
 *
 * @author jipzeongit
 * @since 2023-07-06
 */
@Data
@TableName("inventory")
public class Inventory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("biz_no")
    private String bizNo;

    @TableField("flower_id")
    private Integer flowerId;

    @TableField("biz_type")
    private String bizType;

    private Integer quantity;

    @TableField("before_stock")
    private Integer beforeStock;

    @TableField("after_stock")
    private Integer afterStock;

    @TableField("unit_cost")
    private BigDecimal unitCost;

    @TableField("total_cost")
    private BigDecimal totalCost;

    private String remark;

    @TableField("operator_id")
    private Integer operatorId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date date;

    private Integer deleted;
}
