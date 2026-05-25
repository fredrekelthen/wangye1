package com.edu.backend.service;

import com.edu.backend.model.Material;
import com.edu.backend.model.MaterialChunk;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RAGKnowledgeBaseService {
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    @Autowired
    private GlmApiService glmApiService;
    
    @Value("${glm.api.url}")
    private String glmApiUrl;
    
    @Value("${glm.api.key}")
    private String glmApiKey;
    
    private static final int CHUNK_SIZE = 500; // 每个块的大小（字符数）
    private static final int CHUNK_OVERLAP = 50; // 块重叠大小
    private static final int TOP_K = 5; // 检索返回的最相关块数
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 处理上传的资料：解析、分割、生成向量
     */
    public Material processMaterial(Material material, String content) {
        // 1. 文本分割
        List<MaterialChunk> chunks = splitText(content, material.getId(), material.getTitle());
        
        material.setTotalChunks(chunks.size());
        material.setProcessedChunks(0);
        
        // 2. 为每个块生成向量嵌入
        int processed = 0;
        for (MaterialChunk chunk : chunks) {
            try {
                // 生成向量
                List<Float> embedding = generateEmbedding(chunk.getContent());
                chunk.setEmbedding(embedding);
                chunk.setTokenCount(estimateTokenCount(chunk.getContent()));
                
                // 保存到MongoDB
                mongoTemplate.save(chunk);
                
                processed++;
                material.setProcessedChunks(processed);
                
            } catch (Exception e) {
                System.err.println("处理块失败: " + e.getMessage());
            }
        }
        
        // 3. 更新资料状态
        material.setIsIndexed(true);
        material.setUpdatedAt(LocalDateTime.now());
        mongoTemplate.save(material);
        
        return material;
    }
    
    /**
     * 文本分割为块
     */
    private List<MaterialChunk> splitText(String text, String materialId, String materialTitle) {
        List<MaterialChunk> chunks = new ArrayList<>();
        
        // 按段落分割
        String[] paragraphs = text.split("\n\n+");
        StringBuilder currentChunk = new StringBuilder();
        int currentPosition = 0;
        int chunkIndex = 0;
        
        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) continue;
            
            // 如果当前块加上这个段落超过大小限制，先保存当前块
            if (currentChunk.length() + paragraph.length() > CHUNK_SIZE && currentChunk.length() > 0) {
                MaterialChunk chunk = createChunk(materialId, materialTitle, currentChunk.toString(), chunkIndex, currentPosition);
                chunks.add(chunk);
                
                // 重叠处理：保留最后一部分作为下一个块的开头
                String overlapText = getOverlapText(currentChunk.toString());
                currentChunk = new StringBuilder(overlapText);
                currentPosition += currentChunk.length();
                chunkIndex++;
            }
            
            // 添加段落到当前块
            if (currentChunk.length() > 0) {
                currentChunk.append("\n\n");
            }
            currentChunk.append(paragraph);
        }
        
        // 添加最后一个块
        if (currentChunk.length() > 0) {
            MaterialChunk chunk = createChunk(materialId, materialTitle, currentChunk.toString(), chunkIndex, currentPosition);
            chunks.add(chunk);
        }
        
        return chunks;
    }
    
    private MaterialChunk createChunk(String materialId, String materialTitle, String content, int index, int position) {
        MaterialChunk chunk = new MaterialChunk();
        chunk.setMaterialId(materialId);
        chunk.setMaterialTitle(materialTitle);
        chunk.setChunkIndex(index);
        chunk.setContent(content);
        chunk.setContentHash(computeHash(content));
        chunk.setStartPosition(position);
        chunk.setEndPosition(position + content.length());
        return chunk;
    }
    
    private String getOverlapText(String text) {
        int overlapLength = Math.min(CHUNK_OVERLAP, text.length());
        return text.substring(text.length() - overlapLength);
    }
    
    /**
     * 生成文本的向量嵌入
     */
    public List<Float> generateEmbedding(String text) throws Exception {
        String requestBody = objectMapper.writeValueAsString(Map.of(
            "model", "embedding-3",
            "input", text
        ));
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + glmApiKey);
        
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        
        try {
            String response = restTemplate.postForObject(
                glmApiUrl.replace("/chat/completions", "/embeddings"),
                entity,
                String.class
            );
            
            // 解析返回的向量
            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            List<Map<String, Object>> data = (List<Map<String, Object>>) responseMap.get("data");
            if (data != null && !data.isEmpty()) {
                Map<String, Object> embeddingData = data.get(0);
                List<Number> embedding = (List<Number>) embeddingData.get("embedding");
                return embedding.stream()
                    .map(Number::floatValue)
                    .collect(Collectors.toList());
            }
        } catch (Exception e) {
            System.err.println("使用GLM API生成嵌入失败，使用模拟向量: " + e.getMessage());
            // 如果API调用失败，返回一个模拟向量（用于测试）
            return generateSimulatedEmbedding(text);
        }
        
        return generateSimulatedEmbedding(text);
    }
    
    /**
     * 生成模拟向量（用于API不可用时）
     */
    private List<Float> generateSimulatedEmbedding(String text) {
        // 基于文本内容生成伪随机但一致的向量
        int embeddingSize = 1024;
        List<Float> embedding = new ArrayList<>();
        int seed = text.hashCode();
        Random random = new Random(seed);
        
        for (int i = 0; i < embeddingSize; i++) {
            embedding.add((float) (random.nextDouble() * 2 - 1));
        }
        
        // 归一化
        float norm = (float) Math.sqrt(embedding.stream()
            .map(f -> f * f)
            .reduce(0f, Float::sum));
        
        if (norm > 0) {
            embedding = embedding.stream()
                .map(f -> f / norm)
                .collect(Collectors.toList());
        }
        
        return embedding;
    }
    
    /**
     * 检索最相关的文档块
     */
    public List<MaterialChunk> retrieveRelevantChunks(String query, int topK) {
        try {
            // 1. 生成查询向量
            List<Float> queryEmbedding = generateEmbedding(query);
            
            // 2. 在数据库中查找所有已索引的块
            Query dbQuery = new Query();
            dbQuery.addCriteria(Criteria.where("embedding").exists(true));
            List<MaterialChunk> allChunks = mongoTemplate.find(dbQuery, MaterialChunk.class);
            
            if (allChunks.isEmpty()) {
                return new ArrayList<>();
            }
            
            // 3. 计算余弦相似度并排序
            List<MaterialChunk> scoredChunks = allChunks.stream()
                .map(chunk -> {
                    float similarity = cosineSimilarity(queryEmbedding, chunk.getEmbedding());
                    chunk.setMetadata(Map.of("score", similarity));
                    return chunk;
                })
                .sorted((a, b) -> {
                    Float scoreA = (Float) a.getMetadata().get("score");
                    Float scoreB = (Float) b.getMetadata().get("score");
                    return scoreB.compareTo(scoreA);
                })
                .limit(topK > 0 ? topK : TOP_K)
                .collect(Collectors.toList());
            
            return scoredChunks;
            
        } catch (Exception e) {
            System.err.println("检索失败: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * 计算余弦相似度
     */
    private float cosineSimilarity(List<Float> vec1, List<Float> vec2) {
        if (vec1 == null || vec2 == null || vec1.isEmpty() || vec2.isEmpty()) {
            return 0f;
        }
        
        int size = Math.min(vec1.size(), vec2.size());
        float dotProduct = 0f;
        float norm1 = 0f;
        float norm2 = 0f;
        
        for (int i = 0; i < size; i++) {
            dotProduct += vec1.get(i) * vec2.get(i);
            norm1 += vec1.get(i) * vec1.get(i);
            norm2 += vec2.get(i) * vec2.get(i);
        }
        
        float denominator = (float) (Math.sqrt(norm1) * Math.sqrt(norm2));
        return denominator > 0 ? dotProduct / denominator : 0f;
    }
    
    /**
     * 使用知识库增强的问答
     */
    public String enhancedQA(String question, String defaultResponse) {
        // 1. 检索相关块
        List<MaterialChunk> relevantChunks = retrieveRelevantChunks(question, TOP_K);
        
        if (relevantChunks.isEmpty()) {
            return defaultResponse;
        }
        
        // 2. 构建上下文
        StringBuilder context = new StringBuilder();
        context.append("基于以下资料回答问题：\n\n");
        
        for (int i = 0; i < relevantChunks.size(); i++) {
            MaterialChunk chunk = relevantChunks.get(i);
            context.append("【资料").append(i + 1).append("】(")
                  .append(chunk.getMaterialTitle())
                  .append("):\n")
                  .append(chunk.getContent())
                  .append("\n\n");
        }
        
        context.append("问题：").append(question);
        
        // 3. 使用GLM API生成回答
        String prompt = context.toString();
        
        try {
            return glmApiService.callGlmApi(prompt);
        } catch (Exception e) {
            System.err.println("GLM API调用失败: " + e.getMessage());
            return defaultResponse;
        }
    }
    
    /**
     * 根据章节检索资料
     */
    public List<Material> getMaterialsByChapter(Integer chapterNumber) {
        Query query = new Query();
        query.addCriteria(Criteria.where("chapterNumber").is(chapterNumber));
        query.addCriteria(Criteria.where("isActive").is(true));
        return mongoTemplate.find(query, Material.class);
    }
    
    /**
     * 获取所有资料
     */
    public List<Material> getAllMaterials() {
        Query query = new Query();
        query.addCriteria(Criteria.where("isActive").is(true));
        query.fields().exclude("chunks"); // 不返回chunks以减少数据量
        return mongoTemplate.find(query, Material.class);
    }
    
    /**
     * 删除资料及其关联的块
     */
    public void deleteMaterial(String materialId) {
        // 删除关联的chunks
        Query chunkQuery = new Query(Criteria.where("materialId").is(materialId));
        mongoTemplate.remove(chunkQuery, MaterialChunk.class);
        
        // 删除资料本身
        Query materialQuery = new Query(Criteria.where("id").is(materialId));
        mongoTemplate.remove(materialQuery, Material.class);
    }
    
    /**
     * 估算token数量（简单估算：1 token ≈ 4字符）
     */
    private int estimateTokenCount(String text) {
        return text.length() / 4;
    }
    
    /**
     * 计算文本哈希
     */
    private String computeHash(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return String.valueOf(text.hashCode());
        }
    }
    
    /**
     * 获取知识库统计信息
     */
    public Map<String, Object> getKnowledgeBaseStats() {
        Map<String, Object> stats = new HashMap<>();
        
        long materialCount = mongoTemplate.count(new Query(Criteria.where("isActive").is(true)), Material.class);
        long chunkCount = mongoTemplate.count(new Query(), MaterialChunk.class);
        
        stats.put("totalMaterials", materialCount);
        stats.put("totalChunks", chunkCount);
        stats.put("averageChunksPerMaterial", materialCount > 0 ? (double) chunkCount / materialCount : 0);
        
        return stats;
    }
}
