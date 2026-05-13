package com.edu.backend.service;

import com.edu.backend.model.dto.MaterialQueryRequest;
import com.edu.backend.model.dto.MaterialUploadRequest;
import com.edu.backend.model.entity.Material;

/**
 * 资料服务接口
 */
public interface MaterialService {

    /**
     * 上传资料
     */
    Material uploadMaterial(MaterialUploadRequest materialUploadRequest, String uploaderId);

    /**
     * 获取资料列表（分页）
     */
    Object getMaterialList(MaterialQueryRequest materialQueryRequest);

    /**
     * 根据ID获取资料
     */
    Material getMaterialById(String id);

    /**
     * 根据章节和小节获取资料列表
     */
    java.util.List<Material> getMaterialsByChapterAndLesson(String chapterTitle, String lessonTitle);

    /**
     * 删除资料
     */
    Boolean deleteMaterial(String id, String userId);
}
