package com.edu.backend.service.impl;

import com.edu.backend.model.dto.MaterialQueryRequest;
import com.edu.backend.model.dto.MaterialUploadRequest;
import com.edu.backend.model.entity.Material;
import com.edu.backend.repository.MaterialRepository;
import com.edu.backend.repository.UserRepository;
import com.edu.backend.service.MaterialService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 资料服务实现类
 */
@Slf4j
@Service
public class MaterialServiceImpl implements MaterialService {

    @Resource
    private MaterialRepository materialRepository;

    @Resource
    private UserRepository userRepository;

    /**
     * 上传资料
     */
    @Override
    @Transactional
    public Material uploadMaterial(MaterialUploadRequest materialUploadRequest, String uploaderId) {
        // 1. 校验用户
        if (uploaderId == null || uploaderId.isEmpty()) {
            throw new RuntimeException("用户未登录");
        }

        // 2. 校验文件MD5，检查是否已存在
        String md5 = materialUploadRequest.getMd5();
        if (md5 != null && !md5.isEmpty()) {
            Material existingMaterial = materialRepository.findByMd5(md5);
            if (existingMaterial != null) {
                throw new RuntimeException("文件已存在");
            }
        }

        // 3. 获取上传者信息
        var uploader = userRepository.findById(uploaderId).orElse(null);
        if (uploader == null) {
            throw new RuntimeException("用户不存在");
        }

        // 4. 确定资料类型
        String fileType = materialUploadRequest.getFileType().toLowerCase();
        String materialType;
        switch (fileType) {
            case "pdf":
                materialType = "PDF";
                break;
            case "docx":
                materialType = "DOCX";
                break;
            case "mp4":
                materialType = "视频";
                break;
            case "ppt":
            case "pptx":
                materialType = "PPT";
                break;
            default:
                materialType = fileType.toUpperCase();
        }

        // 5. 创建资料记录
        Material material = new Material();
        material.setFileName(materialUploadRequest.getFileName());
        material.setFileType(fileType);
        material.setMaterialType(materialType);
        material.setFilePath(materialUploadRequest.getFilePath());
        material.setFileSize(materialUploadRequest.getFileSize());
        material.setChapterTitle(materialUploadRequest.getChapterTitle());
        material.setLessonTitle(materialUploadRequest.getLessonTitle());
        material.setPath(materialUploadRequest.getChapterTitle() + "/" + materialUploadRequest.getLessonTitle());
        material.setUploaderId(uploaderId);
        material.setUploaderName(uploader.getUserName() != null ? uploader.getUserName() : uploader.getUserAccount());
        material.setCreateTime(new Date());
        material.setUpdateTime(new Date());
        material.setDownloadCount(0);
        material.setStatus(0); // 0-正常
        material.setDescription(materialUploadRequest.getDescription());
        material.setMd5(md5);
        material.setIsPublic(materialUploadRequest.getIsPublic() != null ? materialUploadRequest.getIsPublic() : true);

        // 6. 保存资料
        Material savedMaterial = materialRepository.save(material);
        log.info("资料上传成功：{} by {}", materialUploadRequest.getFileName(), uploaderId);

        return savedMaterial;
    }

    /**
     * 获取资料列表（分页）
     */
    @Override
    public Object getMaterialList(MaterialQueryRequest materialQueryRequest) {
        Integer pageNum = materialQueryRequest.getPageNum() != null ? materialQueryRequest.getPageNum() : 1;
        Integer pageSize = materialQueryRequest.getPageSize() != null ? materialQueryRequest.getPageSize() : 10;
        String fileName = materialQueryRequest.getFileName();
        String materialType = materialQueryRequest.getMaterialType();
        String chapterTitle = materialQueryRequest.getChapterTitle();
        String lessonTitle = materialQueryRequest.getLessonTitle();
        String uploaderId = materialQueryRequest.getUploaderId();

        PageRequest pageRequest = PageRequest.of(pageNum - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));

        // 根据条件查询
        Page<Material> materialPage;
        if (fileName != null && !fileName.isEmpty()) {
            // 模糊查询文件名
            materialPage = materialRepository.findByFileNameLike(fileName, pageRequest);
        } else if (materialType != null && !materialType.isEmpty()) {
            // 按资料类型查询
            materialPage = materialRepository.findByMaterialType(materialType, pageRequest);
        } else if (uploaderId != null && !uploaderId.isEmpty()) {
            // 按上传者查询
            materialPage = materialRepository.findByUploaderId(uploaderId, pageRequest);
        } else {
            // 查询所有有效资料
            materialPage = materialRepository.findByStatus(0, pageRequest);
        }

        return materialPage;
    }

    /**
     * 根据ID获取资料
     */
    @Override
    public Material getMaterialById(String id) {
        Material material = materialRepository.findById(id).orElse(null);
        if (material == null) {
            throw new RuntimeException("资料不存在");
        }

        // 增加下载计数（这里简单处理，实际可以优化）
        material.setDownloadCount(material.getDownloadCount() + 1);
        materialRepository.save(material);

        return material;
    }

    /**
     * 根据章节和小节获取资料列表
     */
    @Override
    public List<Material> getMaterialsByChapterAndLesson(String chapterTitle, String lessonTitle) {
        if (chapterTitle == null || chapterTitle.isEmpty()) {
            throw new RuntimeException("章节标题不能为空");
        }

        if (lessonTitle != null && !lessonTitle.isEmpty()) {
            // 查询指定章节和小节的资料
            return materialRepository.findByChapterTitleAndLessonTitle(chapterTitle, lessonTitle);
        } else {
            // 查询指定章节的所有资料
            return materialRepository.findByChapterTitle(chapterTitle);
        }
    }

    /**
     * 删除资料
     */
    @Override
    @Transactional
    public Boolean deleteMaterial(String id, String userId) {
        // 1. 校验参数
        if (id == null || id.isEmpty()) {
            throw new RuntimeException("资料ID不能为空");
        }

        // 2. 查询资料
        Material material = materialRepository.findById(id).orElse(null);
        if (material == null) {
            throw new RuntimeException("资料不存在");
        }

        // 3. 校验权限（只有上传者或管理员可以删除）
        if (!material.getUploaderId().equals(userId)) {
            var user = userRepository.findById(userId).orElse(null);
            if (user == null || !"ADMIN".equals(user.getRole())) {
                throw new RuntimeException("无权限删除该资料");
            }
        }

        // 4. 删除资料
        materialRepository.deleteById(id);
        log.info("资料删除成功：id={} by {}", id, userId);

        return true;
    }
}
