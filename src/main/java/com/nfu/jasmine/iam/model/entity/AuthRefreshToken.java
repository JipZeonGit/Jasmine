package com.nfu.jasmine.iam.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("auth_refresh_token")
public class AuthRefreshToken implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    private Integer userId;

    private String tokenId;

    private LocalDateTime expiresAt;

    private Integer revoked;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}