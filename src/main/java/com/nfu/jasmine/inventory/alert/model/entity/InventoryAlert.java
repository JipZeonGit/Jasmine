package com.nfu.jasmine.inventory.alert.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 库存预警读模型实体。
 * <p>
 * 由库存事件消费者驱动 upsert，支撑低库存查询。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("inventory_alert")
public class InventoryAlert implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    @TableField("flower_id")
    private Integer flowerId;

    @TableField("flower_name_snapshot")
    private String flowerNameSnapshot;

    @TableField("safe_stock")
    private Integer safeStock;

    @TableField("current_stock")
    private Integer currentStock;

    @TableField("alert_status")
    private String alertStatus;

    @TableField("last_trigger_time")
    private Date lastTriggerTime;

    @TableField("last_recover_time")
    private Date lastRecoverTime;

    private String remark;
}
