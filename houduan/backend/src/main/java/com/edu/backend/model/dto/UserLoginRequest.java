package com.edu.backend.model.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户登录请求DTO
 */
@Data
public class UserLoginRequest {

    /**
     * 用户账号
     */
    @NotBlank(message = "账号不能为空")
    private String userAccount;

    /**
     * 用户密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度不能少于6位")
    private String userPassword;
}
