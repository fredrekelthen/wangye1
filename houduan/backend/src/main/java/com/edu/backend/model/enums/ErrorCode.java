package com.edu.backend.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码枚举
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 成功
    SUCCESS(0, "success"),

    // 通用错误
    PARAMS_ERROR(40000, "请求参数错误"),
    SYSTEM_ERROR(50000, "系统内部错误"),
    NOT_LOGIN_ERROR(40100, "未登录"),
    NO_AUTH_ERROR(40101, "无权限"),
    NOT_FOUND_ERROR(40400, "请求数据不存在"),

    // 用户相关错误 (40001-40099)
    USER_NOT_EXIST(40001, "用户不存在"),
    USER_ACCOUNT_EXIST(40002, "账号已存在"),
    USER_PASSWORD_ERROR(40003, "密码错误"),
    USER_PASSWORD_SAME(40004, "两次密码输入不一致"),
    USER_PLANET_CODE_EXIST(40005, "编号已存在"),

    // 资料相关错误 (41001-41099)
    MATERIAL_NOT_EXIST(41001, "资料不存在"),
    MATERIAL_UPLOAD_ERROR(41002, "资料上传失败"),
    MATERIAL_TYPE_ERROR(41003, "不支持的文件类型"),
    MATERIAL_SIZE_ERROR(41004, "文件大小超过限制"),
    MATERIAL_FILE_EXIST(41005, "文件已存在"),

    // 课程相关错误 (42001-42099)
    COURSE_NOT_EXIST(42001, "课程不存在"),
    COURSE_STRUCTURE_ERROR(42002, "课程结构错误"),
    ;

    /**
     * 状态码
     */
    private final int code;

    /**
     * 描述信息
     */
    private final String description;

    /**
     * 根据code获取错误码枚举
     */
    public static ErrorCode getByCode(int code) {
        for (ErrorCode errorCode : values()) {
            if (errorCode.getCode() == code) {
                return errorCode;
            }
        }
        return null;
    }
}
