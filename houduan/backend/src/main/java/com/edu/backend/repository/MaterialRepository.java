package com.edu.backend.repository;

import com.edu.backend.model.entity.Material;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 资料数据访问接口
 */
@Repository
public interface MaterialRepository extends MongoRepository<Material, String> {

    /**
     * 根据章节标题查询资料列表
     */
    List<Material> findByChapterTitle(String chapterTitle);

    /**
     * 根据章节和小节标题查询资料列表
     */
    List<Material> findByChapterTitleAndLessonTitle(String chapterTitle, String lessonTitle);

    /**
     * 根据上传者ID查询资料列表（分页）
     */
    Page<Material> findByUploaderId(String uploaderId, Pageable pageable);

    /**
     * 根据资料类型查询资料列表（分页）
     */
    Page<Material> findByMaterialType(String materialType, Pageable pageable);

    /**
     * 根据文件名模糊查询
     */
    @Query("{'fileName': {$regex: ?0, $options: 'i'}}")
    Page<Material> findByFileNameLike(String fileName, Pageable pageable);

    /**
     * 查询有效的资料（status = 0）
     */
    Page<Material> findByStatus(Integer status, Pageable pageable);

    /**
     * 根据文件MD5查询（用于去重）
     */
    Material findByMd5(String md5);
}
