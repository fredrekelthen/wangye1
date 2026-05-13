package com.edu.backend.utils;

public class ResultUtils {
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(0, data, "success");
    }

    public static <T> BaseResponse<T> success(T data, String message) {
        return new BaseResponse<>(0, data, message);
    }

    public static BaseResponse error(int errorCode, String description) {
        return new BaseResponse<>(errorCode, null, description);
    }
}
