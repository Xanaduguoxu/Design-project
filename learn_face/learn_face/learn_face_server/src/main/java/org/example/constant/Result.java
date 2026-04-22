package org.example.constant;

import lombok.Data;

import java.io.Serializable;

@Data
public class Result<T> implements Serializable {

    private Integer code;

    private String message;

    private T data;

    public Result(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success() {
        return new Result<T>(200, "success!");
    }

    public static <T> Result<T> success(T data) {
        return new Result<T>(200, "success!", data);
    }

    public static <T> Result<T> fail() {
        return new Result<T>(200, "fail!");
    }

    public static <T> Result<T> fail(T data) {
        return new Result<T>(200, "fail!", data);
    }
}
