package com.nfu.jasmine.sys.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>
 * 登录响应数据
 * </p>
 *
 * @author jipzeongit
 * @since 2026-04-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {
    private String token;
}
