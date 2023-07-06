package com.nfu.jasmine.cus.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 
 * </p>
 *
 * @author jipzeongit
 * @since 2023-07-06
 */
public class Appointment implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;

    private LocalDateTime date;

    private Integer vid;

    private String name;

    private String sex;

    private String phone;

    private String content;

    private Integer deleted;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }
    public Integer getVid() {
        return vid;
    }

    public void setVid(Integer vid) {
        this.vid = vid;
    }
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }
    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    @Override
    public String toString() {
        return "Appointment{" +
            "id=" + id +
            ", date=" + date +
            ", vid=" + vid +
            ", name=" + name +
            ", sex=" + sex +
            ", phone=" + phone +
            ", content=" + content +
            ", deleted=" + deleted +
        "}";
    }
}
