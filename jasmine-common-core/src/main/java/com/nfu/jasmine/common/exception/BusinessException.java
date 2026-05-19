package com.nfu.jasmine.common.exception;

import com.nfu.jasmine.common.enums.ResultCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final ResultCode resultCode;

    public BusinessException(String message) {
        super(message);
        this.resultCode = ResultCode.BUSINESS_ERROR;
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }
}
