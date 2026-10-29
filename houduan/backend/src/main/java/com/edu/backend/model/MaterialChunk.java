package com.edu.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;
import java.util.Map;

@Document(collection = "material_chunks")
public class MaterialChunk {
    @Id
    private String id;
    
    private String materialId; // 关联的资料ID
    
    private String materialTitle; // 资料标题（冗余存储方便查询）
    
    private Integer chunkIndex; // 块索引
    
    private String content; // 原始文本内容
    
    private String contentHash; // 内容哈希，用于去重
    
    private Integer startPosition; // 在原文中的起始位置
    
    private Integer endPosition; // 在原文中的结束位置
    
    // 向量嵌入
    private List<Float> embedding; // 向量表示

    private String embeddingModel; // 生成向量的模型名称（如 embedding-3），防止不同模型向量混排
    
    private String indexStatus; // 索引状态：READY / PENDING（embedding失败时标记为PENDING等待重试）
    
    private Integer tokenCount; // token数量
    
    private Map<String, Object> metadata; // 额外元数据（页码、段落等）
    
    // Getter and Setter methods
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getMaterialId() {
        return materialId;
    }
    
    public void setMaterialId(String materialId) {
        this.materialId = materialId;
    }
    
    public String getMaterialTitle() {
        return materialTitle;
    }
    
    public void setMaterialTitle(String materialTitle) {
        this.materialTitle = materialTitle;
    }
    
    public Integer getChunkIndex() {
        return chunkIndex;
    }
    
    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getContentHash() {
        return contentHash;
    }
    
    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }
    
    public Integer getStartPosition() {
        return startPosition;
    }
    
    public void setStartPosition(Integer startPosition) {
        this.startPosition = startPosition;
    }
    
    public Integer getEndPosition() {
        return endPosition;
    }
    
    public void setEndPosition(Integer endPosition) {
        this.endPosition = endPosition;
    }
    
    public List<Float> getEmbedding() {
        return embedding;
    }
    
    public void setEmbedding(List<Float> embedding) {
        this.embedding = embedding;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public String getIndexStatus() {
        return indexStatus;
    }

    public void setIndexStatus(String indexStatus) {
        this.indexStatus = indexStatus;
    }
    
    public Integer getTokenCount() {
        return tokenCount;
    }
    
    public void setTokenCount(Integer tokenCount) {
        this.tokenCount = tokenCount;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
