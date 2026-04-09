package com.nfu.jasmine.cus.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class InventoryVO {
    private Integer id;
    private String name;
    private String num;
    private Integer quantity;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date date;

    private Integer residue;
}