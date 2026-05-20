package com.nfu.jasmine.common.dto.internal;

import lombok.Data;

/**
 * 用户基本信息 DTO，用于服务间内部通信。
 */
@Data
public class UserBasicDTO {
    private Integer id;
    private String username;
    private String realName;
}
