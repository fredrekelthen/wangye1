package com.edu.backend.exception;

import com.edu.backend.model.enums.ErrorCode;
import com.edu.backend.utils.BaseResponse;
import com.edu.backend.utils.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 全局异常处理器：统一异常出口。
 *
 * <p>对外只返回泛化信息（错误码 + 友好提示），细节与堆栈只写入日志，
 * 杜绝把 {@code e.getMessage()} / 堆栈泄露给客户端。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常：code={}, message={}", e.getCode(), e.getMessage());
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = fieldMessage(e.getBindingResult().getFieldError());
        log.warn("参数校验失败：{}", message);
        return ResultUtils.error(ErrorCode.PARAMS_ERROR.getCode(), message);
    }

    @ExceptionHandler(BindException.class)
    public BaseResponse<Void> handleBindException(BindException e) {
        String message = fieldMessage(e.getBindingResult().getFieldError());
        return ResultUtils.error(ErrorCode.PARAMS_ERROR.getCode(), message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public BaseResponse<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return ResultUtils.error(ErrorCode.PARAMS_ERROR.getCode(), "缺少参数：" + e.getParameterName());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public BaseResponse<Void> handleMaxUpload(MaxUploadSizeExceededException e) {
        log.warn("上传文件超过大小限制：{}", e.getMessage());
        return ResultUtils.error(ErrorCode.MATERIAL_SIZE_ERROR.getCode(), "文件超过大小限制");
    }

    @ExceptionHandler(Exception.class)
    public BaseResponse<Void> handleException(Exception e) {
        log.error("未处理异常", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR.getCode(), "服务器内部错误，请稍后重试");
    }

    private String fieldMessage(FieldError fieldError) {
        if (fieldError == null || fieldError.getDefaultMessage() == null) {
            return "请求参数错误";
        }
        return fieldError.getDefaultMessage();
    }
}
