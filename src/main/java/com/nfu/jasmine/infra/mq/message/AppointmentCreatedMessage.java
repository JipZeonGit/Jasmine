package com.nfu.jasmine.infra.mq.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 预约创建消息体，在预约事务提交后发布，消费端用于触发通知（短信、微信等）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentCreatedMessage implements Serializable {
    private Integer appointmentId;
    private Integer vipId;
    private String vipName;
    private String vipPhone;
    private Date appointmentTime;
    private String content;
    private Date occurredAt;
}