package org.example.config.security;

import cn.hutool.json.JSONUtil;
import org.example.constant.Result;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static org.example.constant.ResponseEnum.NO_PERMISSION;


/**
 * 功能描述: 权限认证失败处理器
 */
@Component
public class AccessDeniedHandlerImp implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AccessDeniedException e) throws IOException, ServletException {
        httpServletResponse.setContentType("application/json;charset=utf-8");
        PrintWriter out = httpServletResponse.getWriter();
        out.write(JSONUtil.toJsonStr(Result.fail(NO_PERMISSION)));
        out.flush();
        out.close();
    }
}

