package com.nfu.jasmine.infra.notification.model.entity;

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
 * 站内通知消息实体。
 * <p>
 * 由 MQ 消费者（如预约提醒）异步写入，前端轮询展示给店员。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("site_message")
public class SiteMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 业务类型，如 APPOINTMENT_REMINDER */
    @TableField("biz_type")
    private String bizType;

    /** 业务主键，用于消费端回查和前端跳转 */
    @TableField("biz_id")
    private String bizId;

    /** 站内信标题 */
    private String title;

    /** 站内信正文 */
    private String content;

    /** 0=未读 1=已读 */
    @TableField("is_read")
    private Integer isRead;

    /** 消息生成时间 */
    @TableField("created_at")
    private Date createdAt;
}
