package com.nfu.jasmine.infra.outbox.model.entity;

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
 * 本地消息表（Outbox）实体。
 * <p>
 * 主事务内写入此表，由独立 Relay 任务异步扫描并发 MQ，
 * 保证"主业务成功则事件不丢"。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("event_outbox")
public class EventOutbox implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("event_type")
    private String eventType;

    @TableField("exchange")
    private String exchange;

    @TableField("routing_key")
    private String routingKey;

    @TableField("payload")
    private String payload;

    @TableField("status")
    private String status;

    @TableField("retry_count")
    private Integer retryCount;

    @TableField("next_retry_time")
    private Date nextRetryTime;

    @TableField("last_error")
    private String lastError;

    @TableField("created_at")
    private Date createdAt;

    @TableField("sent_at")
    private Date sentAt;
}
