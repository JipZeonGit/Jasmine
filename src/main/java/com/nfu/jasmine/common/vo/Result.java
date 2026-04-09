package com.nfu.jasmine.common.vo;

import com.nfu.jasmine.common.enums.ResultCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private Integer code;
    private String message;
    private T data;

    public static <T> Result<T> success(){
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }
    public static <T> Result<T> success(T data){
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }
    public static <T> Result<T> success(T data,String message){
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data);
    }
    public static <T> Result<T> success(String message){
        return new Result<>(ResultCode.SUCCESS.getCode(), message, null);
    }
    public static<T>  Result<T> fail(){
        return fail(ResultCode.BUSINESS_ERROR);
    }

    public static<T>  Result<T> fail(Integer code){
        return new Result<>(code, ResultCode.BUSINESS_ERROR.getMessage(), null);
    }

    public static<T>  Result<T> fail(Integer code, String message){
        return new Result<>(code,message,null);
    }

    public static<T>  Result<T> fail( String message){
        return new Result<>(ResultCode.BUSINESS_ERROR.getCode(), message, null);
    }

    public static <T> Result<T> fail(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static <T> Result<T> fail(ResultCode resultCode, String message) {
        return new Result<>(resultCode.getCode(), message, null);
    }
}
