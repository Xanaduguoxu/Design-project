package org.example.config.security;

import cn.hutool.json.JSONUtil;
import org.example.constant.ResponseEnum;
import org.example.constant.Result;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static org.example.constant.ResponseEnum.*;


@Component
public class AuthenticationEntryPointImp implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException {
        response.setContentType("application/json;charset=utf-8");
        PrintWriter out = response.getWriter();
        if (e instanceof InsufficientAuthenticationException) {
            printf(out, NOT_LOGIN);
        } else if (e instanceof BadCredentialsException) {
            printf(out, PASSWORD_ERROR);
        } else if (e instanceof InternalAuthenticationServiceException) {
            printf(out, USERNAME_NULL);
        }
    }

    private void printf(PrintWriter pw, ResponseEnum responseEnum) {
        pw.write(JSONUtil.toJsonStr(Result.fail(responseEnum.getDescribe())));
        pw.flush();
        pw.close();
    }
}

