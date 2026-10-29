package com.edu.backend.exception;

import com.edu.backend.model.enums.ErrorCode;

/**
 * 业务异常：携带业务错误码与对外提示信息，由 {@link GlobalExceptionHandler} 统一处理。
 *
 * <p>业务校验失败应抛出本异常，避免在各 Controller 中手写 try/catch、以及把
 * {@code e.getMessage()} 原样返回给客户端造成内部信息泄露。</p>
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDescription());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
