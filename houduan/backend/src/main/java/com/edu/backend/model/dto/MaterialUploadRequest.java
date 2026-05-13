package com.edu.backend.model.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 资料上传请求DTO
 */
@Data
public class MaterialUploadRequest {

    /**
     * 文件名
     */
    @NotBlank(message = "文件名不能为空")
    private String fileName;

    /**
     * 文件类型（扩展名）
     */
    private String fileType;

    /**
     * 文件路径/URL
     */
    @NotBlank(message = "文件路径不能为空")
    private String filePath;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 所属章节标题
     */
    @NotBlank(message = "章节标题不能为空")
    private String chapterTitle;

    /**
     * 所属小节标题
     */
    @NotBlank(message = "小节标题不能为空")
    private String lessonTitle;

    /**
     * 资料描述
     */
    private String description;

    /**
     * 文件MD5
     */
    private String md5;

    /**
     * 是否公开
     */
    private Boolean isPublic;
}
