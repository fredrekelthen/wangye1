package com.edu.backend.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * 学习资料实体
 * MongoDB Collection: materials
 * 用于管理课程相关的PDF、DOCX、视频、PPT等学习资料
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "materials")
public class Material {

    /**
     * 主键
     */
    @Id
    private String id;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件类型（扩展名）：pdf, docx, mp4, ppt, pptx
     */
    private String fileType;

    /**
     * 资料类型：PDF, DOCX, 视频, PPT
     */
    private String materialType;

    /**
     * 文件路径/URL
     */
    private String filePath;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 所属章节标题（如：第1章 矢量分析）
     */
    @Indexed
    private String chapterTitle;

    /**
     * 所属小节标题（如：1.1 矢量代数）
     */
    @Indexed
    private String lessonTitle;

    /**
     * 完整路径（如：第1章 矢量分析/1.1 矢量代数）
     */
    private String path;

    /**
     * 上传者ID
     */
    @Indexed
    private String uploaderId;

    /**
     * 上传者名称
     */
    private String uploaderName;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 下载次数
     */
    private Integer downloadCount;

    /**
     * 资料状态：NORMAL(正常), DELETED(已删除)
     */
    private Integer status;

    /**
     * 资料描述
     */
    private String description;

    /**
     * 文件MD5（用于去重）
     */
    private String md5;

    /**
     * 是否公开：true(公开), false(私有)
     */
    private Boolean isPublic;
}
