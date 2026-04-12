package com.nfu.jasmine.appointment.web.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class AppointmentVO {
    private Integer id;
    private Integer vipId;
    private String vid;
    private String name;
    private String sex;
    private String phone;
    private String content;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date date;
}
