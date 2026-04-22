package org.example.config;

import lombok.extern.slf4j.Slf4j;
import org.example.config.security.LoginUser;
import org.example.config.security.SecurityWhiteListProperties;
import org.example.utils.JwtUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtFilter extends OncePerRequestFilter {

    @Resource
    private SecurityWhiteListProperties whiteListProperties;

    //  全局异常捕获
    @Autowired
    @Qualifier("handlerExceptionResolver")
    private HandlerExceptionResolver resolver;

    private final PathMatcher pathMatcher = new AntPathMatcher();

    private List<String> cachedWhiteList;

    @PostConstruct
    public void init() {
        this.cachedWhiteList = whiteListProperties.getUrls().stream().filter(item -> !item.equals("/chat/chats")).map(item -> "/v1" + item).collect(Collectors.toList());
    }

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain) throws ServletException, IOException {
        //  白名单
        for (String pattern : cachedWhiteList) {
            if (pathMatcher.match(pattern, request.getRequestURI())) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        //  2.获取token
        String token = request.getHeader("Authorization");
        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            log.info("未携带token");
            return;
        }

        LoginUser loginUser;
        try {
            loginUser = JwtUtils.parseToken(token);
        } catch (Exception e) {
            throw new RuntimeException("token非法");
        }

        if (loginUser == null) {
            filterChain.doFilter(request, response);
            return;
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}