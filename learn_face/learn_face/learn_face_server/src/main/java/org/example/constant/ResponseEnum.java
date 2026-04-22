package org.example.constant;

import lombok.Getter;


@Getter
public enum ResponseEnum {

    NOT_LOGIN(411, "未登陆,请先登陆!"),

    USERNAME_NULL(412, "用户名为空!"),

    PASSWORD_ERROR(413, "用户名或密码错误!"),

    USER_EXIST(414, "用户名已存在!"),

    SUCCESS(200, "操作成功!"),

    ERROR(401, "操作失败!"),

    NO_PERMISSION(415, "权限不足,无法查阅资源!");

    private final Integer code;

    private final String describe;

    ResponseEnum(Integer code, String describe) {
        this.code = code;
        this.describe = describe;
    }
}
