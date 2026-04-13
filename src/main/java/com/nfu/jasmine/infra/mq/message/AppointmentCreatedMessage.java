package com.nfu.jasmine.infra.mq.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

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