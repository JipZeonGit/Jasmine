package com.nfu.jasmine.common.dto.internal;

import lombok.Data;

/**
 * 会员基本信息 DTO，用于服务间内部通信。
 */
@Data
public class VipBasicDTO {
    private Integer id;
    private String vid;
    private String name;
    private String sex;
    private String phone;
}
