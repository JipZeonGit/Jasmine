package com.nfu.jasmine.cus.entity;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * <p>
 * 
 * </p>
 *
 * @author jipzeongit
 * @since 2023-07-06
 */
@Data
public class Appointment implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;

    @DateTimeFormat(pattern = "yyyy-MM-dd hh:mm:ss")
    private Date date;

    private Integer vid;

    private String name;

    private String sex;

    private String phone;

    private String content;

    private Integer deleted;
}
