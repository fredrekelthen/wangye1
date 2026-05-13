package com.edu.backend.utils;

import lombok.Data;
import java.io.Serializable;

@Data
public class BaseResponse<T> implements Serializable {
    private int code;
    private T data;
    private String description;

    public BaseResponse(int code, T data, String description) {
        this.code = code;
        this.data = data;
        this.description = description;
    }
}
