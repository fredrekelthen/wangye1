package com.edu.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "materials")
public class Material {
    @Id
    private String id;
    
    @Indexed
    private String title;
    
    private String description;
    
    private String fileName;
    
    private String fileType; // pdf, docx
    
    private String fileUrl;
    
    private Long fileSize;
    
    @Indexed
    private String category; // 章节分类
    
    private Integer chapterNumber;
    
    private String uploadedBy; // 上传者用户名
    
    private LocalDateTime uploadedAt;
    
    private LocalDateTime updatedAt;
    
    private Boolean isActive = true;
    
    // RAG相关字段
    private Integer totalChunks; // 总块数
    private Integer processedChunks; // 已处理的块数
    private Boolean isIndexed = false; // 是否已完成向量索引
    
    private Map<String, Object> metadata; // 额外元数据
    
    // Getter and Setter methods
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getFileType() {
        return fileType;
    }
    
    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
    
    public String getFileUrl() {
        return fileUrl;
    }
    
    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }
    
    public Long getFileSize() {
        return fileSize;
    }
    
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public Integer getChapterNumber() {
        return chapterNumber;
    }
    
    public void setChapterNumber(Integer chapterNumber) {
        this.chapterNumber = chapterNumber;
    }
    
    public String getUploadedBy() {
        return uploadedBy;
    }
    
    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }
    
    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }
    
    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public Integer getTotalChunks() {
        return totalChunks;
    }
    
    public void setTotalChunks(Integer totalChunks) {
        this.totalChunks = totalChunks;
    }
    
    public Integer getProcessedChunks() {
        return processedChunks;
    }
    
    public void setProcessedChunks(Integer processedChunks) {
        this.processedChunks = processedChunks;
    }
    
    public Boolean getIsIndexed() {
        return isIndexed;
    }
    
    public void setIsIndexed(Boolean isIndexed) {
        this.isIndexed = isIndexed;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
