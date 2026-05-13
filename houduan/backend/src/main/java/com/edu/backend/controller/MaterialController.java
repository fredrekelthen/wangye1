package com.edu.backend.controller;

import com.edu.backend.model.dto.MaterialQueryRequest;
import com.edu.backend.model.dto.MaterialUploadRequest;
import com.edu.backend.model.entity.Material;
import com.edu.backend.service.MaterialService;
import com.edu.backend.utils.BaseResponse;
import com.edu.backend.utils.ResultUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;
import java.io.IOException;

import java.util.List;

/**
 * 资料控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/material")
public class MaterialController {

    @Resource
    private MaterialService materialService;

    /**
     * 上传资料
     */
    @PostMapping("/upload")
    public BaseResponse<Material> uploadMaterial(
            @RequestBody MaterialUploadRequest materialUploadRequest,
            HttpServletRequest request
    ) {
        try {
            String userId = (String) request.getSession().getAttribute("userId");
            Material material = materialService.uploadMaterial(materialUploadRequest, userId);
            return ResultUtils.success(material, "上传成功");
        } catch (Exception e) {
            log.error("资料上传失败：{}", e.getMessage());
            return ResultUtils.error(40000, e.getMessage());
        }
    }

    /**
     * 上传文件（存储到服务器并返回访问URL/元数据）
     */
    @PostMapping("/upload-file")
    public BaseResponse<Map<String, Object>> uploadFile(@RequestParam("file") MultipartFile file,
                                                        HttpServletRequest request) {
        try {
            if (file == null || file.isEmpty()) {
                return ResultUtils.error(40001, "文件不能为空");
            }

            // 存储目录（相对于运行目录的 uploads 文件夹）
            String uploadDir = System.getProperty("user.dir") + "/uploads";

            // 使用工具类保存并计算 md5
            String savedFileName = com.edu.backend.utils.FileStorageUtil.saveFile(file, uploadDir);
            String md5 = com.edu.backend.utils.FileStorageUtil.calculateMD5(file);

            String baseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            String filePath = baseUrl + "/uploads/" + savedFileName;

            Map<String, Object> result = new HashMap<>();
            result.put("filePath", filePath);
            result.put("fileName", file.getOriginalFilename());
            String orig = file.getOriginalFilename();
            String ext = "";
            if (orig != null && orig.contains(".")) {
                ext = orig.substring(orig.lastIndexOf('.') + 1).toLowerCase();
            }
            result.put("fileType", ext);
            result.put("fileSize", file.getSize());
            result.put("md5", md5);

            return ResultUtils.success(result, "文件上传成功");
        } catch (IOException e) {
            log.error("文件保存失败：{}", e.getMessage());
            return ResultUtils.error(50000, "文件保存失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("文件上传异常：{}", e.getMessage());
            return ResultUtils.error(50000, e.getMessage());
        }
    }

    /**
     * 获取资料列表（分页）
     */
    @GetMapping("/list")
    public BaseResponse<Object> getMaterialList(MaterialQueryRequest materialQueryRequest) {
        try {
            Object materialList = materialService.getMaterialList(materialQueryRequest);
            return ResultUtils.success(materialList);
        } catch (Exception e) {
            log.error("获取资料列表失败：{}", e.getMessage());
            return ResultUtils.error(40000, e.getMessage());
        }
    }

    /**
     * 根据ID获取资料
     */
    @GetMapping("/{id}")
    public BaseResponse<Material> getMaterialById(@PathVariable String id) {
        try {
            Material material = materialService.getMaterialById(id);
            return ResultUtils.success(material);
        } catch (Exception e) {
            log.error("获取资料失败：{}", e.getMessage());
            return ResultUtils.error(40000, e.getMessage());
        }
    }

    /**
     * 根据章节和小节获取资料列表
     */
    @GetMapping("/by-chapter")
    public BaseResponse<List<Material>> getMaterialsByChapterAndLesson(
            @RequestParam String chapterTitle,
            @RequestParam(required = false) String lessonTitle
    ) {
        try {
            List<Material> materials = materialService.getMaterialsByChapterAndLesson(chapterTitle, lessonTitle);
            return ResultUtils.success(materials);
        } catch (Exception e) {
            log.error("获取资料列表失败：{}", e.getMessage());
            return ResultUtils.error(40000, e.getMessage());
        }
    }

    /**
     * 删除资料
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteMaterial(@RequestParam String id, HttpServletRequest request) {
        try {
            String userId = (String) request.getSession().getAttribute("userId");
            Boolean result = materialService.deleteMaterial(id, userId);
            return ResultUtils.success(result, "删除成功");
        } catch (Exception e) {
            log.error("删除资料失败：{}", e.getMessage());
            return ResultUtils.error(40000, e.getMessage());
        }
    }
}
