package com.tzy.sky.dto.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> implements Serializable {
    private int code;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data, LocalDateTime.now());
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data, LocalDateTime.now());
    }

    public static <T> Result<T> success(String message) {
        return new Result<>(200, message, null, LocalDateTime.now());
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null, LocalDateTime.now());
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null, LocalDateTime.now());
    }

    public static <T> Result<T> badRequest(String message) {
        return new Result<>(400, message, null, LocalDateTime.now());
    }

    public static <T> Result<T> notFound(String message) {
        return new Result<>(404, message, null, LocalDateTime.now());
    }

    public static <T> Result<T> unauthorized(String message) {
        return new Result<>(401, message, null, LocalDateTime.now());
    }

    public static <T> Result<T> forbidden(String message) {
        return new Result<>(403, message, null, LocalDateTime.now());
    }
}