package com.nfu.jasmine.common.enums;

import lombok.Getter;

/**
 * 统一返回状态码定义
 *
 * @author jipzeongit
 * @since 2026-04-09
 */
@Getter
public enum ResultCode {
    SUCCESS(20000, "success"),
    BUSINESS_ERROR(20001, "fail"),
    LOGIN_ERROR(20002, "用户名或密码错误！"),
    UNAUTHORIZED(20003, "请先登录后再访问！"),
    FORBIDDEN(20004, "当前用户无权访问该资源！"),
    VALIDATE_FAILED(20005, "请求参数校验失败！"),
    NOT_FOUND(20006, "目标数据不存在！"),
    CONFLICT(20007, "数据冲突，请检查后重试！"),
    SERVER_ERROR(50000, "系统异常，请稍后重试！");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}