package com.nfu.jasmine.sales.model.entity;

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
 * 销售单头
 * </p>
 *
 * @author jipzeongit
 * @since 2023-07-06
 */
@Data
@TableName("sales")
public class Sales implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("order_no")
    private String orderNo;

    @TableField("vip_id")
    private Integer vipId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date date;

    @TableField("total_amount")
    private BigDecimal totalAmount;

    private String remark;

    @TableField("operator_id")
    private Integer operatorId;

    private Integer deleted;
}
