package com.nfu.jasmine.sys.vo;

import com.nfu.jasmine.sys.entity.Menu;
import lombok.Data;

import java.util.List;

/**
 * <p>
 * 当前登录用户信息
 * </p>
 *
 * @author jipzeongit
 * @since 2026-04-07
 */
@Data
public class UserInfoVO {
    private String name;
    private String avatar;
    private String phone;
    private String email;
    private Integer status;
    private List<String> roles;
    private List<Menu> menuList;
}
