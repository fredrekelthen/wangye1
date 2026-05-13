package com.edu.backend.model.dto;

import lombok.Data;

/**
 * 资料查询请求DTO
 */
@Data
public class MaterialQueryRequest {

    /**
     * 资料ID
     */
    private String id;

    /**
     * 文件名（模糊查询）
     */
    private String fileName;

    /**
     * 资料类型
     */
    private String materialType;

    /**
     * 所属章节标题
     */
    private String chapterTitle;

    /**
     * 所属小节标题
     */
    private String lessonTitle;

    /**
     * 上传者ID
     */
    private String uploaderId;

    /**
     * 当前页码
     */
    private Integer pageNum;

    /**
     * 每页数量
     */
    private Integer pageSize;
}
