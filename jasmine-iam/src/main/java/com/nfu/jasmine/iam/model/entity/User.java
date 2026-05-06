package com.nfu.jasmine.iam.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.nfu.jasmine.common.model.LoginUserInfo;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 
 * </p>
 *
 * @author jipzeongit
 * @since 2023-05-29
 */

@Data
public class User implements Serializable, LoginUserInfo {
    private static final long serialVersionUID = 1L;
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private String username;
    private String password;
    private String email;
    private String phone;
    private Integer status;
    private String avatar;
    private Integer deleted;

    @TableField(exist = false)
    private List<Integer> roleIdList;

//    // 新增字段来存储密码哈希值
//    @TableField(exist = false)
//    private String passwordHash;
//
//    // 设置密码哈希值
//    public void setPasswordHash(String passwordHash) {
//        this.passwordHash = passwordHash;
//    }
//
//    // 更新密码哈希值
//    public void changePassword(String newPasswordHash) {
//        this.passwordHash = newPasswordHash;
//    }

}
