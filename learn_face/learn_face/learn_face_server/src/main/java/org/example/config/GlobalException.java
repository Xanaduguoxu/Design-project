package org.example.config;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 功能描述: 全局异常处理
 */
@Slf4j
@RestControllerAdvice
public class GlobalException {

    /**
     * 功能描述: 其它异常捕获
     *
     * @param response
     * @param e
     * @return void
     */
    @ExceptionHandler(Exception.class)
    public void exception(HttpServletResponse response, Exception e) {
        ByteArrayOutputStream exception = new ByteArrayOutputStream();
        e.printStackTrace(new PrintStream(exception));
        log.error(exception.toString());
        output(response, e.getMessage(), 200);
    }

    @ExceptionHandler(RuntimeException.class)
    public void runtimeException(HttpServletResponse response, RuntimeException e) {
        ByteArrayOutputStream exception = new ByteArrayOutputStream();
        e.printStackTrace(new PrintStream(exception));
        log.error(exception.toString());
        if (e.getMessage() == null) {
            output(response, "系统异常", 200);
        } else {
            output(response, e.getMessage(), 200);
        }
    }


    /**
     * 功能描述: 错误返回
     *
     * @param response
     * @param message
     * @return void
     */
    private void output(HttpServletResponse response, String message, Integer code) {
        try {
            response.setContentType("application/json;charset=utf-8");
            ServletOutputStream out = response.getOutputStream();
            Map<String, Object> map = new HashMap<>();
            map.put("code", code);
            map.put("message", message);
            out.write(JSONUtil.toJsonStr(map).getBytes());
            out.flush();
            out.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
