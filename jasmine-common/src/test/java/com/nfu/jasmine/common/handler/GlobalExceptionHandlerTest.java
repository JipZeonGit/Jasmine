package com.nfu.jasmine.common.handler;

import com.nfu.jasmine.common.enums.ResultCode;
import com.nfu.jasmine.common.exception.BusinessException;
import com.nfu.jasmine.common.vo.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private GlobalExceptionHandler handler;

    @Test
    void shouldHandleBusinessException() {
        BusinessException ex = new BusinessException(ResultCode.CONFLICT, "库存不足");

        BusinessExceptionHandler businessHandler = new BusinessExceptionHandler();
        Result<Object> result = businessHandler.handleBusinessException(ex, request);

        assertThat(result.getCode()).isEqualTo(ResultCode.CONFLICT.getCode());
        assertThat(result.getMessage()).isEqualTo("库存不足");
    }

    @Test
    void shouldHandleTimeoutException() {
        TimeoutException ex = new TimeoutException("connect timed out");

        Result<Object> result = handler.handleTimeoutException(ex, request);

        assertThat(result.getCode()).isEqualTo(ResultCode.SERVER_ERROR.getCode());
        assertThat(result.getMessage()).contains("超时");
    }

    @Test
    void shouldHandleConstraintViolationException() {
        ConstraintViolationException ex = new ConstraintViolationException("name must not be blank", null);

        Result<Object> result = handler.handleConstraintViolationException(ex, request);

        assertThat(result.getCode()).isEqualTo(ResultCode.VALIDATE_FAILED.getCode());
        assertThat(result.getMessage()).isEqualTo("name must not be blank");
    }

    @Test
    void shouldHandleMissingParameterException() {
        MissingServletRequestParameterException ex =
                new MissingServletRequestParameterException("flowerId", "Integer");

        Result<Object> result = handler.handleMissingServletRequestParameterException(ex, request);

        assertThat(result.getCode()).isEqualTo(ResultCode.VALIDATE_FAILED.getCode());
        assertThat(result.getMessage()).contains("flowerId");
    }

    @Test
    void shouldHandleTypeMismatchException() {
        MethodArgumentTypeMismatchException ex =
                new MethodArgumentTypeMismatchException("abc", Integer.class, "id", null, null);

        Result<Object> result = handler.handleMethodArgumentTypeMismatchException(ex, request);

        assertThat(result.getCode()).isEqualTo(ResultCode.VALIDATE_FAILED.getCode());
        assertThat(result.getMessage()).contains("id");
    }

    @Test
    void shouldHandleDataIntegrityViolationException() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Duplicate entry");

        Result<Object> result = handler.handleDataIntegrityViolationException(ex, request);

        assertThat(result.getCode()).isEqualTo(ResultCode.CONFLICT.getCode());
        assertThat(result.getMessage()).contains("唯一约束");
    }

    @Test
    void shouldHandleGenericException() {
        Exception ex = new RuntimeException("something went wrong");

        Result<Object> result = handler.handleException(ex, request);

        assertThat(result.getCode()).isEqualTo(ResultCode.SERVER_ERROR.getCode());
    }

    @Test
    void shouldHandleBindException() {
        BindException ex = new BindException();
        FieldError fieldError = new FieldError("dto", "name", "不能为空");
        ex.addError(fieldError);

        Result<Object> result = handler.handleBindException(ex, request);

        assertThat(result.getCode()).isEqualTo(ResultCode.VALIDATE_FAILED.getCode());
        assertThat(result.getMessage()).isEqualTo("不能为空");
    }

    @Test
    void shouldHandleValidationExceptionWithNullFieldError() {
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null);

        Result<Object> result = handler.handleMethodArgumentNotValidException(ex, request);

        assertThat(result.getCode()).isEqualTo(ResultCode.VALIDATE_FAILED.getCode());
    }
}
