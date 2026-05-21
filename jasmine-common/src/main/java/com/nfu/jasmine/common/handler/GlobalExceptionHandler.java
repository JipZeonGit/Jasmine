package com.nfu.jasmine.common.handler;

import com.nfu.jasmine.common.enums.ResultCode;
import com.nfu.jasmine.common.exception.BusinessException;
import com.nfu.jasmine.common.vo.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.concurrent.TimeoutException;

/**
 * 全局异常处理器。
 * <p>
 * 覆盖参数校验、数据库约束、业务异常、远程调用超时、断路器熔断等场景。
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

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<Object> handleDataIntegrityViolationException(DataIntegrityViolationException e, HttpServletRequest request) {
        log.warn("数据库约束冲突 method={} uri={} message={}", request.getMethod(), request.getRequestURI(), e.getMostSpecificCause().getMessage());
        return Result.fail(ResultCode.CONFLICT, "数据违反唯一约束或关联约束，请检查后重试！");
    }

    /**
     * 远程调用超时 —— RestClient 或 WebClient 调用下游服务超时。
     */
    @ExceptionHandler(TimeoutException.class)
    public Result<Object> handleTimeoutException(TimeoutException e, HttpServletRequest request) {
        log.error("远程调用超时 method={} uri={}", request.getMethod(), request.getRequestURI(), e);
        return Result.fail(ResultCode.SERVER_ERROR, "服务调用超时，请稍后重试！");
    }

    /**
     * Resilience4j 断路器熔断 —— 下游服务持续不可用时触发。
     * <p>
     * 异常全限定名：io.github.resilience4j.circuitbreaker.CallNotPermittedException
     * 通过 Exception 类名字符串匹配，避免 common 模块直接依赖 resilience4j。
     */
    @ExceptionHandler(Exception.class)
    public Result<Object> handleException(Exception e, HttpServletRequest request) {
        // 断路器熔断异常特殊处理：返回 503 语义，让前端提示"服务暂时不可用"
        if ("io.github.resilience4j.circuitbreaker.CallNotPermittedException".equals(e.getClass().getName())) {
            log.error("断路器熔断 method={} uri={}", request.getMethod(), request.getRequestURI(), e);
            return Result.fail(ResultCode.SERVER_ERROR, "下游服务暂时不可用，请稍后重试！");
        }
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
