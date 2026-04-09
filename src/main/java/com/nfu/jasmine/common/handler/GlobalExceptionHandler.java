package com.nfu.jasmine.common.handler;

import com.nfu.jasmine.common.enums.ResultCode;
import com.nfu.jasmine.common.vo.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * <p>
 * 全局异常处理器
 * </p>
 *
 * @author jipzeongit
 * @since 2026-04-07
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Object> handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = getFieldErrorMessage(e.getBindingResult().getFieldError());
        log.warn("参数校验失败 method={} uri={} message={}", request.getMethod(), request.getRequestURI(), message);
        return Result.fail(ResultCode.VALIDATE_FAILED, message);
    }

    @ExceptionHandler(BindException.class)
    public Result<Object> handleBindException(BindException e, HttpServletRequest request) {
        String message = getFieldErrorMessage(e.getBindingResult().getFieldError());
        log.warn("绑定参数失败 method={} uri={} message={}", request.getMethod(), request.getRequestURI(), message);
        return Result.fail(ResultCode.VALIDATE_FAILED, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Object> handleConstraintViolationException(ConstraintViolationException e, HttpServletRequest request) {
        log.warn("约束校验失败 method={} uri={} message={}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return Result.fail(ResultCode.VALIDATE_FAILED, e.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Object> handleMissingServletRequestParameterException(MissingServletRequestParameterException e, HttpServletRequest request) {
        String message = e.getParameterName() + "参数不能为空！";
        log.warn("请求参数缺失 method={} uri={} message={}", request.getMethod(), request.getRequestURI(), message);
        return Result.fail(ResultCode.VALIDATE_FAILED, message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Object> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String message = e.getName() + "参数格式不正确！";
        log.warn("参数类型不匹配 method={} uri={} message={}", request.getMethod(), request.getRequestURI(), message);
        return Result.fail(ResultCode.VALIDATE_FAILED, message);
    }

    @ExceptionHandler(Exception.class)
    public Result<Object> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常 method={} uri={}", request.getMethod(), request.getRequestURI(), e);
        return Result.fail(ResultCode.SERVER_ERROR);
    }

    private String getFieldErrorMessage(FieldError fieldError) {
        if (fieldError != null) {
            return fieldError.getDefaultMessage();
        }
        return ResultCode.VALIDATE_FAILED.getMessage();
    }
}