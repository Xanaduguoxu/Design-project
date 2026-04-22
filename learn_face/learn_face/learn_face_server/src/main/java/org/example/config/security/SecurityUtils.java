package org.example.config.security;

import cn.hutool.core.bean.BeanUtil;
import org.example.mapper.UserMapper;
import org.example.pojo.po.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 功能描述:security工具类
 */
public class SecurityUtils {

    @Resource
    private static UserMapper userMapper;

    /**
     * 功能描述: 密码加密
     */
    public static String getPassword(String password) {
        BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
        return bCryptPasswordEncoder.encode(password);
    }

    /**
     * 功能描述: 获取LoginUser
     */
    public static LoginUser getLoginUser() {
        try {
            UsernamePasswordAuthenticationToken authentication = (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
            return (LoginUser) authentication.getPrincipal();
        } catch (ClassCastException e) {
            Map<String, Object> map = BeanUtil.beanToMap(SecurityContextHolder.getContext().getAuthentication().getPrincipal());
            map.entrySet().removeIf(next -> next.getValue() == null);
            return BeanUtil.toBean(map, LoginUser.class);
        }
    }

    /**
     * 功能描述: 获取用户名
     */
    public static String getUsername() {
        return getLoginUser().getUsername();
    }

    /**
     * 功能描述: 获取用户id
     */
    public static Long getUserId() {
        return getLoginUser().getId();
    }

    public static String getRole() {
        return getLoginUser().getRole();
    }

    public static User getUser() {
        Long id = getUserId();
        return userMapper.selectById(id);
    }
}
