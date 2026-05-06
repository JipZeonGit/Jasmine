package com.nfu.jasmine.common;

import com.nfu.jasmine.common.exception.BusinessException;
import com.nfu.jasmine.common.vo.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class BusinessExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(BusinessExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public Result<Object> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("Business validation failed method={} uri={} message={}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return Result.fail(e.getResultCode(), e.getMessage());
    }
}
