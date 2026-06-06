package com.springbank.common.dto;

import java.time.LocalDateTime;

public record ApiResponse<T>(
    boolean success,
    String message,
    T data,
    int status,
    String path,
    LocalDateTime timestamp
) {
    public static <T> ApiResponse<T> success(String message, T data, String path) {
        return new ApiResponse<>(true, message, data, 200, path, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> error(String message, int status, String path) {
        return new ApiResponse<>(false, message, null, status, path, LocalDateTime.now());
    }
}
