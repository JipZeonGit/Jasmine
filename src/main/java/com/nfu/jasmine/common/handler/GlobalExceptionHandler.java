package com.nfu.jasmine.common.handler;

import com.nfu.jasmine.common.enums.ResultCode;
import com.nfu.jasmine.common.vo.Result;
import jakarta.validation.ConstraintViolationException;
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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Object> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        return Result.fail(ResultCode.VALIDATE_FAILED, getFieldErrorMessage(e.getBindingResult().getFieldError()));
    }

    @ExceptionHandler(BindException.class)
    public Result<Object> handleBindException(BindException e) {
        return Result.fail(ResultCode.VALIDATE_FAILED, getFieldErrorMessage(e.getBindingResult().getFieldError()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Object> handleConstraintViolationException(ConstraintViolationException e) {
        return Result.fail(ResultCode.VALIDATE_FAILED, e.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Object> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        return Result.fail(ResultCode.VALIDATE_FAILED, e.getParameterName() + "参数不能为空！");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Object> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        return Result.fail(ResultCode.VALIDATE_FAILED, e.getName() + "参数格式不正确！");
    }

    @ExceptionHandler(Exception.class)
    public Result<Object> handleException(Exception e) {
        e.printStackTrace();
        return Result.fail(ResultCode.SERVER_ERROR);
    }

    private String getFieldErrorMessage(FieldError fieldError) {
        if (fieldError != null) {
            return fieldError.getDefaultMessage();
        }
        return ResultCode.VALIDATE_FAILED.getMessage();
    }
}