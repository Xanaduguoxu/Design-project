package org.example.config.security;

import org.example.config.JwtFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.annotation.Resource;
import java.util.List;


@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private SecurityWhiteListProperties whiteListProperties;

    // 自动调用config.UserDetailsServiceImp
    @Resource
    private UserDetailsService userDetailsService;

    // 设置权限
    @Autowired
    private AccessDeniedHandler accessDeniedHandler;

    // 未登录
    @Autowired
    private AuthenticationEntryPoint authEntryPoint;

    @Autowired
    private JwtFilter jwtFilter;

    // 注入BCryptPasswordEncoder,SpringSecurity会使用PasswordEncoder自动密码校验
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 用户权限管理器，进行用户认证，配置用户签名服务和用户权限控制
    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        // 不通过Session获取SecurityContext
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        //关闭csrf
        http.csrf().disable();

        // 解决跨域
        http.cors();

        // 未登录
        http.exceptionHandling().authenticationEntryPoint(authEntryPoint);

        // 设置权限，对应hasAnyAuthority
        http.exceptionHandling().accessDeniedHandler(accessDeniedHandler);

        List<String> whiteUrls = whiteListProperties.getUrls();
        String[] whiteArray = whiteUrls.toArray(new String[0]);

        // 设置认证的action
        http.authorizeRequests()
                //  auth下的操作一律不拦截
                //  "/auth/sendCode", "/auth/login", "/auth/register", "/auth/findPassword", "/common/**", "/chat/chats","/webSocket/**"
                .antMatchers(whiteArray).permitAll()
                .anyRequest().authenticated();

        //  设置过滤器
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
    }
}
