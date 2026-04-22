package org.example.config.security;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.mapper.common.AuthMapper;
import org.example.pojo.po.common.Auth;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 功能描述: security 登陆逻辑
 */
@Component
public class UserDetailsServiceHandler implements UserDetailsService {

    @Resource
    private AuthMapper authMapper;

    @Override
    public UserDetails loadUserByUsername(String account) throws UsernameNotFoundException {
        if (StrUtil.isBlank(account)) {
            throw new BadCredentialsException("用户名为空!");
        }

        Auth auth = authMapper.selectOne(new LambdaQueryWrapper<Auth>().eq(Auth::getUsername, account));
        if (ObjectUtil.isNull(auth)) {
            throw new BadCredentialsException("用户名不存在!");
        }

        //  权限
        List<String> authorities = new ArrayList<>(Collections.singletonList(auth.getRole()));

        return new LoginUser(auth.getId(), auth.getUsername(), auth.getPassword(), auth.getRole(), authorities, 0, 0, 0, 0);
    }
}
