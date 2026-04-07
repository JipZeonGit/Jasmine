package com.nfu.jasmine.handler;

import com.alibaba.fastjson2.JSON;
import com.nfu.jasmine.common.vo.Result;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException, ServletException {
        response.setContentType("application/json;charset=utf-8");
        Result<Object> fail = Result.fail(20004, "当前用户无权访问该资源！");
        response.getWriter().write(JSON.toJSONString(fail));
    }
}
