package com.nfu.jasmine.appointment.web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
public class AppointmentCreateDTO {
    private Integer vipId;
    private String vid;
    private String phone;

    @NotNull(message = "预约时间不能为空！")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date date;

    @NotBlank(message = "预约内容不能为空！")
    private String content;
}
