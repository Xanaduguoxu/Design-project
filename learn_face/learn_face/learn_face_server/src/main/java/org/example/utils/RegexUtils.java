package org.example.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 功能描述: 正则表达式工具类
 *
 * @author zb
 * @date 2024/3/9
 * @return
 */
public class RegexUtils {

    /**
     * 功能描述: 邮箱校验,true表示为邮箱,false反之
     *
     * @param email
     * @return boolean
     * @author zb
     * @date 2024/1/28
     */
    public static boolean checkEmail(String email) {
        String regex = "^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(.[a-zA-Z0-9_-]+)+$";
        Pattern compile = Pattern.compile(regex);
        Matcher matcher = compile.matcher(email);
        return matcher.matches();
    }

    /**
     * 功能描述: 电话校验,true表示为电话,false反之
     *
     * @param phone
     * @return boolean
     * @author zb
     * @date 2024/1/28
     */
    public static boolean checkPhone(String phone) {
        String regex = "^((13[0-9])|(14[5,7])|(15[0-3,5-9])|(17[0,3,5-8])|(18[0-9])|166|198|199|(147))\\d{8}$";
        Pattern compile = Pattern.compile(regex);
        Matcher matcher = compile.matcher(phone);
        return matcher.matches();
    }
}
