package com.nfu.jasmine.interceptor;

import com.alibaba.fastjson2.JSON;
import com.nfu.jasmine.common.utils.JwtUtil;
import com.nfu.jasmine.common.vo.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
public class JwtValidateInterceptor implements HandlerInterceptor {
    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("X-Token"); // 请求头
        log.debug(request.getRequestURI() + " 需要验证：" + token);
        if (token != null) {
            try {
                jwtUtil.parseToken(token);
                log.debug(request.getRequestURI() + " 验证通过");
                return true; // 通过
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        log.debug(request.getRequestURI() + " 验证失败，禁止访问");
        response.setContentType("application/json;charset=utf-8");
        Result<Object> fail = Result.fail(20003, "JWT无效，请重新登录！");
        response.getWriter().write(JSON.toJSONString(fail));
        return false; // 拦截
    }
}
