package com.edu.backend.model.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 删除用户请求DTO
 */
@Data
public class UserDeleteRequest {

    /**
     * 用户ID
     */
    @NotBlank(message = "用户ID不能为空")
    private String id;
}
