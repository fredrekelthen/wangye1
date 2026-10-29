package com.edu.backend.controller;

import com.edu.backend.model.Material;
import com.edu.backend.service.FileParsingService;
import com.edu.backend.service.RAGKnowledgeBaseService;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/materials")
@CrossOrigin(origins = "*")
public class MaterialController {
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    @Autowired
    private RAGKnowledgeBaseService ragService;
    
    @Autowired
    private FileParsingService fileParsingService;
    
    @Value("${upload.directory:uploads}")
    private String uploadDirectory;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 上传资料
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadMaterial(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "chapterNumber", required = false) Integer chapterNumber,
            @RequestParam(value = "sectionTitle", required = false) String sectionTitle,
            @RequestParam(value = "subsectionTitle", required = false) String subsectionTitle,
            @RequestParam(value = "uploadedBy", required = false) String uploadedBy) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 验证文件
            if (!fileParsingService.isValidFileType(file)) {
                response.put("success", false);
                response.put("message", "不支持的文件类型，请上传PDF、Word或TXT文件");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (!fileParsingService.isValidFileSize(file)) {
                response.put("success", false);
                response.put("message", "文件大小超过限制（最大10MB）");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 一次读取字节，同时用于存盘和解析，避免重复读流
            byte[] fileBytes = file.getBytes();
            String originalName = file.getOriginalFilename();
            String fileName = UUID.randomUUID().toString() + "_" + originalName;
            Path uploadPath = Paths.get(uploadDirectory);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(new java.io.ByteArrayInputStream(fileBytes), filePath);
            
            // 解析文件内容（传入 bytes 避免重复读流）
            String content = fileParsingService.parseFile(fileBytes, originalName);
            
            // 创建资料对象
            Material material = new Material();
            material.setId(UUID.randomUUID().toString());
            material.setTitle(title != null ? title : file.getOriginalFilename());
            material.setDescription(description);
            material.setFileName(file.getOriginalFilename());
            material.setFileType(getFileType(file.getOriginalFilename()));
            material.setFileUrl(filePath.toString());
            material.setFileSize(file.getSize());
            material.setCategory(category);
            material.setChapterNumber(chapterNumber);
            material.setSectionTitle(sectionTitle);
            material.setSubsectionTitle(subsectionTitle);
            material.setUploadedBy(uploadedBy != null ? uploadedBy : "admin");
            material.setUploadedAt(LocalDateTime.now());
            material.setUpdatedAt(LocalDateTime.now());
            material.setIsActive(true);
            material.setIsIndexed(false);
            
            // 保存资料到MongoDB
            mongoTemplate.save(material);
            
            // 在后台处理RAG索引
            new Thread(() -> {
                try {
                    ragService.processMaterial(material, content);
                } catch (Exception e) {
                    log.warn("RAG处理失败: {}", e.getMessage());
                }
            }).start();
            
            response.put("success", true);
            response.put("message", "资料上传成功，正在后台建立索引...");
            response.put("material", convertToMap(material));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "上传失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 获取所有资料列表
     */
    @GetMapping("/list")
    public ResponseEntity<Map<String, Object>> getMaterials(
            @RequestParam(value = "chapter", required = false) Integer chapter) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Query query = new Query();
            query.addCriteria(Criteria.where("isActive").is(true));
            if (chapter != null) {
                query.addCriteria(Criteria.where("chapterNumber").is(chapter));
            }
            
            List<Material> materials = mongoTemplate.find(query, Material.class);
            
            // 如果没有资料，返回空列表而不是错误
            response.put("success", true);
            response.put("materials", materials);
            response.put("total", materials.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取资料列表失败", e);
            response.put("success", false);
            response.put("message", "获取资料列表失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 获取单个资料详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getMaterial(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Query query = new Query(Criteria.where("id").is(id));
            Material material = mongoTemplate.findOne(query, Material.class);
            
            if (material == null) {
                response.put("success", false);
                response.put("message", "资料不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            response.put("success", true);
            response.put("material", convertToMap(material));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取资料详情失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 更新资料信息
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateMaterial(
            @PathVariable String id,
            @RequestBody Map<String, Object> updates) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            Query query = new Query(Criteria.where("id").is(id));
            Material material = mongoTemplate.findOne(query, Material.class);
            
            if (material == null) {
                response.put("success", false);
                response.put("message", "资料不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
            
            // 更新字段
            if (updates.containsKey("title")) {
                material.setTitle((String) updates.get("title"));
            }
            if (updates.containsKey("description")) {
                material.setDescription((String) updates.get("description"));
            }
            if (updates.containsKey("category")) {
                material.setCategory((String) updates.get("category"));
            }
            if (updates.containsKey("chapterNumber")) {
                material.setChapterNumber((Integer) updates.get("chapterNumber"));
            }
            
            material.setUpdatedAt(LocalDateTime.now());
            mongoTemplate.save(material);
            
            response.put("success", true);
            response.put("message", "资料更新成功");
            response.put("material", convertToMap(material));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "更新失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 删除资料
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteMaterial(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 删除文件
            Query query = new Query(Criteria.where("id").is(id));
            Material material = mongoTemplate.findOne(query, Material.class);
            
            if (material != null && material.getFileUrl() != null) {
                Path filePath = Paths.get(material.getFileUrl());
                Files.deleteIfExists(filePath);
            }
            
            // 删除数据库记录（RAG服务会删除关联的chunks）
            ragService.deleteMaterial(id);
            
            response.put("success", true);
            response.put("message", "资料删除成功");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "删除失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 访问（预览/下载）资料文件。
     * 文件存储于服务器本地磁盘（fileUrl 为物理路径），通过本接口以 HTTP 流返回，
     * 浏览器可直接预览 PDF 或下载 Word/TXT 文件。
     */
    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> getMaterialFile(@PathVariable String id) {
        try {
            Query query = new Query(Criteria.where("id").is(id));
            Material material = mongoTemplate.findOne(query, Material.class);
            if (material == null || material.getFileUrl() == null) {
                return ResponseEntity.notFound().build();
            }
            Path path = Paths.get(material.getFileUrl());
            if (!Files.exists(path)) {
                return ResponseEntity.notFound().build();
            }
            Resource resource = new InputStreamResource(Files.newInputStream(path));
            String contentType = resolveContentType(material.getFileType());
            String fileName = material.getFileName() != null ? material.getFileName() : "download";
            String disposition = "inline; filename=\"" + URLEncoder.encode(fileName, "UTF-8") + "\"";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    private String resolveContentType(String fileType) {
        if (fileType == null) return "application/octet-stream";
        switch (fileType.toLowerCase()) {
            case "pdf":
                return "application/pdf";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "doc":
                return "application/msword";
            case "txt":
            case "md":
                return "text/plain; charset=UTF-8";
            default:
                return "application/octet-stream";
        }
    }
    
    /**
     * 获取知识库统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Map<String, Object> stats = new HashMap<>();
            try {
                stats = ragService.getKnowledgeBaseStats();
            } catch (Exception e) {
                // 如果获取统计信息失败，返回默认值
                stats.put("totalMaterials", 0);
                stats.put("totalChunks", 0);
                stats.put("averageChunksPerMaterial", 0);
            }
            
            response.put("success", true);
            response.put("stats", stats);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("获取资料统计失败", e);
            // 即使出错也返回默认值
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalMaterials", 0);
            stats.put("totalChunks", 0);
            stats.put("averageChunksPerMaterial", 0);
            response.put("success", true);
            response.put("stats", stats);
            
            return ResponseEntity.ok(response);
        }
    }
    
    /**
     * 重建 chunk 向量索引。
     *
     * @param full {@code true} = 全量重算（含由旧 embedding 模型生成的向量，
     *             更换 embedding 模型后必须使用）；{@code false} = 仅重试 PENDING
     */
    @PostMapping("/reindex")
    public ResponseEntity<Map<String, Object>> reindexPending(
            @RequestParam(value = "full", required = false, defaultValue = "false") boolean full) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> result = ragService.reindexChunks(full);
            response.put("success", true);
            response.put("result", result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "重建索引失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 基于知识库回答问题
     */
    @PostMapping("/query")
    public ResponseEntity<Map<String, Object>> queryKnowledgeBase(
            @RequestBody Map<String, String> request) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String question = request.get("question");
            if (question == null || question.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "问题不能为空");
                return ResponseEntity.badRequest().body(response);
            }
            
            String defaultResponse = "抱歉，我没有找到相关的资料来回答您的问题。";
            Map<String, Object> ragResult = ragService.enhancedQA(question, defaultResponse);
            
            response.put("success", true);
            response.put("question", question);
            response.put("answer", ragResult.get("answer"));
            response.put("citations", ragResult.getOrDefault("citations", Collections.emptyList()));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "查询失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 获取按章节分组的资料列表
     */
    @GetMapping("/by-chapter")
    public ResponseEntity<Map<String, Object>> getMaterialsByChapter() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Material> allMaterials = ragService.getAllMaterials();
            
            // 按章节分组
            Map<Integer, List<Material>> materialsByChapter = new TreeMap<>();
            for (Material material : allMaterials) {
                Integer chapter = material.getChapterNumber();
                if (chapter == null) {
                    chapter = 0;
                }
                materialsByChapter.computeIfAbsent(chapter, k -> new ArrayList<>()).add(material);
            }
            
            response.put("success", true);
            response.put("materialsByChapter", materialsByChapter);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取资料列表失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 根据章节生成学习内容
     */
    @GetMapping("/generate-content/{chapter}")
    public ResponseEntity<Map<String, Object>> generateChapterContent(@PathVariable Integer chapter) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<Material> materials = ragService.getMaterialsByChapter(chapter);
            
            if (materials.isEmpty()) {
                response.put("success", false);
                response.put("message", "该章节暂无资料，请先上传资料");
                return ResponseEntity.ok(response);
            }
            
            // 构建提示词
            StringBuilder prompt = new StringBuilder();
            prompt.append("基于以下资料，为第").append(chapter).append("章生成完整的学习内容：\n\n");
            
            for (Material material : materials) {
                prompt.append("【").append(material.getTitle()).append("】\n");
                prompt.append(material.getDescription()).append("\n\n");
            }
            
            prompt.append("请生成包含以下部分的学习内容：\n");
            prompt.append("1. 章节概述\n");
            prompt.append("2. 核心知识点\n");
            prompt.append("3. 重点难点\n");
            prompt.append("4. 学习建议\n");
            prompt.append("5. 相关习题\n");
            
            response.put("success", true);
            response.put("chapter", chapter);
            response.put("materials", materials.size());
            response.put("prompt", prompt.toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "生成学习内容失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 按章节生成学习内容（全量召回该章所有片段后交给大模型）。
     *
     * <p>与 {@code POST /query} 的区别：{@code /query} 走 top-K 语义检索，只适合
     * 精确问答；"生成整章学习内容"需要覆盖整章，故使用本接口。</p>
     */
    @PostMapping("/generate-chapter/{chapter}")
    public ResponseEntity<Map<String, Object>> generateChapterStudyContent(
            @PathVariable Integer chapter) {

        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, Object> generated = ragService.generateChapterStudyContent(chapter);

            if (generated.get("materialCount") instanceof Integer count && count == 0) {
                response.put("success", false);
                response.put("message", "该章节暂无资料，请先上传资料");
                return ResponseEntity.ok(response);
            }

            String answer = generated.get("answer") == null ? "" : generated.get("answer").toString();
            if (answer.isBlank()) {
                response.put("success", false);
                response.put("message", "生成失败，请稍后重试");
                return ResponseEntity.ok(response);
            }

            response.put("success", true);
            response.put("chapter", chapter);
            response.put("answer", answer);
            response.put("materialCount", generated.get("materialCount"));
            response.put("chunkCount", generated.get("chunkCount"));
            response.put("citations", generated.getOrDefault("citations", Collections.emptyList()));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "生成学习内容失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 辅助方法：获取文件类型
     */
    private String getFileType(String fileName) {
        if (fileName == null) return "unknown";
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".pdf")) return "pdf";
        if (lowerName.endsWith(".docx")) return "docx";
        if (lowerName.endsWith(".doc")) return "doc";
        if (lowerName.endsWith(".txt")) return "txt";
        if (lowerName.endsWith(".md")) return "md";
        return "unknown";
    }
    
    /**
     * 辅助方法：转换对象为Map
     */
    private Map<String, Object> convertToMap(Material material) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", material.getId());
        map.put("title", material.getTitle());
        map.put("description", material.getDescription());
        map.put("fileName", material.getFileName());
        map.put("fileType", material.getFileType());
        map.put("fileSize", material.getFileSize());
        map.put("category", material.getCategory());
        map.put("chapterNumber", material.getChapterNumber());
        map.put("sectionTitle", material.getSectionTitle());
        map.put("subsectionTitle", material.getSubsectionTitle());
        map.put("uploadedBy", material.getUploadedBy());
        map.put("uploadedAt", material.getUploadedAt());
        map.put("updatedAt", material.getUpdatedAt());
        map.put("isIndexed", material.getIsIndexed());
        return map;
    }
}
