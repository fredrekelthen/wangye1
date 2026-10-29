package com.edu.backend.service;

import com.edu.backend.llm.LlmCallOptions;
import com.edu.backend.model.Material;
import com.edu.backend.model.MaterialChunk;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RAGKnowledgeBaseService {
    
    @Autowired
    private MongoTemplate mongoTemplate;
    
    @Autowired
    private GlmApiService glmApiService;

    /**
     * 大模型门面：按章生成这类长任务需要显式指定 max_tokens。
     * （deepseek-flash 为推理模型，思考与回答共享 max_tokens 额度，
     * 默认的 800 会被思考过程耗尽，导致正式回答为空。）
     */
    @Autowired
    private LlmService llmService;

    /** 本地 Embedding 服务客户端（替代 GLM embedding-3） */
    @Autowired
    private EmbeddingClient embeddingClient;

    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 50;
    private static final int TOP_K = 5;
    private static final int HYBRID_RECALL_SIZE = 20; // 混合检索时每种方法召回的数量
    /** 按章生成学习内容时，拼入上下文的字符上限（整章全量片段，约 6 万字） */
    private static final int CHAPTER_CONTEXT_MAX_CHARS = 60000;
    /**
     * 按章生成学习内容的 max_tokens。
     *
     * <p>deepseek-flash 为推理模型，思考过程与正式回答<b>共享</b>该额度：
     * 生成整章内容时思考通常消耗数千 tokens，若沿用默认的 800，
     * 思考就会耗尽额度，导致 finish_reason=length 且正式回答为空。</p>
     */
    private static final int CHAPTER_GENERATE_MAX_TOKENS = 16000;
    private static final int RRF_K = 60; // RRF 融合参数
    private static final double BM25_K1 = 1.5;
    private static final double BM25_B = 0.75;
    /** 向量模型标识：本地 bge-small-zh-v1.5（见 EmbeddingClient），用于区分不同模型产生的向量 */
    private static final String EMBEDDING_MODEL_NAME = EmbeddingClient.MODEL_NAME;

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // ========== BM25 内存倒排索引 ==========
    // term -> {chunkMongoId -> term_frequency_in_that_chunk}
    private final Map<String, Map<String, Integer>> invertedIndex = new HashMap<>();
    // chunkMongoId -> document_length_in_terms
    private final Map<String, Integer> docLengths = new HashMap<>();
    private double avgDocLength = 0;
    private int totalDocs = 0;
    
    /**
     * 启动时从 MongoDB 重建 BM25 索引
     */
    @PostConstruct
    public void initBM25Index() {
        try {
            Query query = new Query();
            query.addCriteria(Criteria.where("indexStatus").is("READY"));
            List<MaterialChunk> chunks = mongoTemplate.find(query, MaterialChunk.class);
            for (MaterialChunk chunk : chunks) {
                if (chunk.getId() != null && chunk.getContent() != null) {
                    addToBM25Index(chunk.getId(), chunk.getContent());
                }
            }
            log.info("[BM25] 启动索引构建完成，共索引 " + totalDocs + " 个chunk");
        } catch (Exception e) {
            log.warn("[BM25] 启动索引构建失败: " + e.getMessage());
        }
    }
    
    /**
     * 将单个chunk加入BM25倒排索引
     */
    private void addToBM25Index(String chunkId, String content) {
        List<String> terms = tokenize(content);
        Map<String, Integer> termFreq = new HashMap<>();
        for (String term : terms) {
            termFreq.merge(term, 1, Integer::sum);
        }
        for (Map.Entry<String, Integer> entry : termFreq.entrySet()) {
            invertedIndex.computeIfAbsent(entry.getKey(), k -> new HashMap<>())
                .put(chunkId, entry.getValue());
        }
        docLengths.put(chunkId, terms.size());
        totalDocs++;
        avgDocLength = docLengths.values().stream().mapToInt(Integer::intValue).average().orElse(0);
    }
    
    /**
     * 从BM25索引中移除一个chunk
     */
    private void removeFromBM25Index(String chunkId) {
        if (!docLengths.containsKey(chunkId)) return;
        // 清理倒排索引中的引用（代价较高，适合低频删除）
        for (Iterator<Map.Entry<String, Map<String, Integer>>> it = invertedIndex.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, Map<String, Integer>> entry = it.next();
            entry.getValue().remove(chunkId);
            if (entry.getValue().isEmpty()) {
                it.remove();
            }
        }
        docLengths.remove(chunkId);
        totalDocs--;
        avgDocLength = totalDocs > 0
            ? docLengths.values().stream().mapToInt(Integer::intValue).average().orElse(0)
            : 0;
    }
    
    /**
     * 中文+英文分词：中文按字切（unigram），英文按非字母数字字符分割后转小写
     */
    private List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null || text.isEmpty()) return tokens;
        
        StringBuilder currentWord = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                // 英文字母数字：继续累积
                currentWord.append(Character.toLowerCase(c));
            } else if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                // 中文：先flush当前英文词，再把每个中文字作为独立token
                if (currentWord.length() > 0) {
                    tokens.add(currentWord.toString());
                    currentWord.setLength(0);
                }
                tokens.add(String.valueOf(c));
            } else {
                // 标点/空格：flush英文词，标点本身忽略
                if (currentWord.length() > 0) {
                    tokens.add(currentWord.toString());
                    currentWord.setLength(0);
                }
            }
        }
        if (currentWord.length() > 0) {
            tokens.add(currentWord.toString());
        }
        return tokens;
    }
    
    /**
     * BM25 评分：对单个chunk计算与query的相关性得分
     */
    private double bm25Score(String chunkId, List<String> queryTerms) {
        if (!docLengths.containsKey(chunkId)) return 0;
        int docLen = docLengths.get(chunkId);
        double score = 0;
        
        for (String term : queryTerms) {
            Map<String, Integer> posting = invertedIndex.get(term);
            if (posting == null) continue;
            
            Integer tf = posting.get(chunkId);
            if (tf == null) continue;
            
            // IDF
            int df = posting.size(); // document frequency
            double idf = Math.log((totalDocs - df + 0.5) / (df + 0.5) + 1.0);
            
            // TF normalization
            double numerator = tf * (BM25_K1 + 1);
            double denominator = tf + BM25_K1 * (1 - BM25_B + BM25_B * docLen / avgDocLength);
            
            score += idf * numerator / denominator;
        }
        return score;
    }
    
    /**
     * BM25 关键词检索：返回 topN 个chunk，按BM25得分降序
     */
    public List<MaterialChunk> bm25Search(String query, int topN) {
        List<String> queryTerms = tokenize(query);
        if (queryTerms.isEmpty() || totalDocs == 0) return new ArrayList<>();
        
        Set<String> candidateIds = new HashSet<>();
        for (String term : queryTerms) {
            Map<String, Integer> posting = invertedIndex.get(term);
            if (posting != null) {
                candidateIds.addAll(posting.keySet());
            }
        }
        
        if (candidateIds.isEmpty()) return new ArrayList<>();
        
        // 批量从MongoDB加载chunk
        Query dbQuery = new Query(Criteria.where("id").in(candidateIds));
        List<MaterialChunk> chunks = mongoTemplate.find(dbQuery, MaterialChunk.class);
        Map<String, MaterialChunk> chunkMap = chunks.stream()
            .collect(Collectors.toMap(MaterialChunk::getId, c -> c));
        
        // 计算BM25得分
        for (MaterialChunk chunk : chunks) {
            double score = bm25Score(chunk.getId(), queryTerms);
            chunk.setMetadata(new HashMap<>(chunk.getMetadata() != null ? chunk.getMetadata() : Map.of()));
            chunk.getMetadata().put("bm25Score", (float) score);
        }
        
        chunks.sort((a, b) -> {
            Float sa = (Float) a.getMetadata().get("bm25Score");
            Float sb = (Float) b.getMetadata().get("bm25Score");
            return sb.compareTo(sa);
        });
        
        return chunks.subList(0, Math.min(topN, chunks.size()));
    }
    
    /**
     * RRF (Reciprocal Rank Fusion) 融合BM25和向量检索结果
     * RRF(d, q) = Σ 1/(k + rank_i(d, q))
     */
    private List<MaterialChunk> rrfFusion(List<MaterialChunk> vectorResults, List<MaterialChunk> bm25Results, int topK) {
        // chunkId -> RRF score accumulator
        Map<String, Double> rrfScores = new HashMap<>();
        Map<String, MaterialChunk> chunkMap = new HashMap<>();
        
        // 向量结果：按顺序分配rank
        for (int i = 0; i < vectorResults.size(); i++) {
            MaterialChunk c = vectorResults.get(i);
            if (c.getId() == null) continue;
            double score = 1.0 / (RRF_K + i + 1);
            rrfScores.merge(c.getId(), score, Double::sum);
            chunkMap.putIfAbsent(c.getId(), c);
        }
        
        // BM25结果：按顺序分配rank
        for (int i = 0; i < bm25Results.size(); i++) {
            MaterialChunk c = bm25Results.get(i);
            if (c.getId() == null) continue;
            double score = 1.0 / (RRF_K + i + 1);
            rrfScores.merge(c.getId(), score, Double::sum);
            chunkMap.putIfAbsent(c.getId(), c);
        }
        
        // 按RRF得分排序
        return rrfScores.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(topK)
            .map(entry -> {
                MaterialChunk chunk = chunkMap.get(entry.getKey());
                if (chunk.getMetadata() == null) {
                    chunk.setMetadata(new HashMap<>());
                }
                chunk.getMetadata().put("rrfScore", entry.getValue().floatValue());
                return chunk;
            })
            .collect(Collectors.toList());
    }
    
    // ========== 资料处理 ==========
    
    /**
     * 处理上传的资料：解析、分割、生成向量
     */
    public Material processMaterial(Material material, String content) {
        List<MaterialChunk> chunks = splitText(content, material.getId(), material.getTitle());
        
        material.setTotalChunks(chunks.size());
        material.setProcessedChunks(0);
        
        int processed = 0;
        int pending = 0;
        for (MaterialChunk chunk : chunks) {
            try {
                List<Float> embedding = generateEmbedding(chunk.getContent());
                chunk.setEmbedding(embedding);
                chunk.setEmbeddingModel(EMBEDDING_MODEL_NAME);
                chunk.setIndexStatus("READY");
                chunk.setTokenCount(estimateTokenCount(chunk.getContent()));
                
                mongoTemplate.save(chunk);
                
                // 更新BM25索引
                addToBM25Index(chunk.getId(), chunk.getContent());
                
                processed++;
                material.setProcessedChunks(processed);
                
            } catch (Exception e) {
                log.warn("[RAG] 块处理失败（已标记PENDING）: chunk#" + chunk.getChunkIndex() + " - " + e.getMessage());
                chunk.setIndexStatus("PENDING");
                chunk.setTokenCount(estimateTokenCount(chunk.getContent()));
                // PENDING的chunk不设embedding和embeddingModel，也先存库
                mongoTemplate.save(chunk);
                pending++;
            }
        }
        
        material.setUpdatedAt(LocalDateTime.now());
        if (pending == 0) {
            material.setIsIndexed(true);
        }
        mongoTemplate.save(material);
        
        if (pending > 0) {
            log.info("[RAG] 资料处理完成：" + processed + "个READY，" + pending + "个PENDING待重试");
        }
        
        return material;
    }
    
    /**
     * 重试处理 PENDING 状态的chunk（后台调用）
     */
    public int retryPendingChunks(String materialId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("materialId").is(materialId));
        query.addCriteria(Criteria.where("indexStatus").is("PENDING"));
        List<MaterialChunk> pendingChunks = mongoTemplate.find(query, MaterialChunk.class);
        
        int success = 0;
        for (MaterialChunk chunk : pendingChunks) {
            try {
                List<Float> embedding = generateEmbedding(chunk.getContent());
                chunk.setEmbedding(embedding);
                chunk.setEmbeddingModel(EMBEDDING_MODEL_NAME);
                chunk.setIndexStatus("READY");
                mongoTemplate.save(chunk);
                addToBM25Index(chunk.getId(), chunk.getContent());
                success++;
            } catch (Exception e) {
                log.warn("[RAG] 重试失败: chunk#" + chunk.getChunkIndex() + " - " + e.getMessage());
            }
        }
        return success;
    }

    /**
     * 全局重建：仅重试 PENDING 状态的 chunk。
     */
    public Map<String, Object> retryAllPendingChunks() {
        return reindexChunks(false);
    }

    /**
     * 重建 chunk 向量索引。
     *
     * @param full {@code true} = 全量重算：除 PENDING 外，还包括由其他 embedding 模型
     *             生成的旧向量（更换 embedding 模型后必须用本模式，否则旧向量维度不同、
     *             无法与新查询向量比对）；
     *             {@code false} = 仅重试 PENDING 状态的 chunk
     * @return 成功、失败、总计的统计
     */
    public Map<String, Object> reindexChunks(boolean full) {
        Query query = new Query();
        if (full) {
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("indexStatus").is("PENDING"),
                    Criteria.where("embeddingModel").ne(EMBEDDING_MODEL_NAME)));
        } else {
            query.addCriteria(Criteria.where("indexStatus").is("PENDING"));
        }
        List<MaterialChunk> pendingChunks = mongoTemplate.find(query, MaterialChunk.class);

        int success = 0;
        int failed = 0;
        for (MaterialChunk chunk : pendingChunks) {
            try {
                List<Float> embedding = generateEmbedding(chunk.getContent());
                chunk.setEmbedding(embedding);
                chunk.setEmbeddingModel(EMBEDDING_MODEL_NAME);
                chunk.setIndexStatus("READY");
                mongoTemplate.save(chunk);
                addToBM25Index(chunk.getId(), chunk.getContent());
                success++;
            } catch (Exception e) {
                log.warn("[RAG] retryAllPending 失败: chunk#" + chunk.getChunkIndex() + " - " + e.getMessage());
                failed++;
            }
        }

        // 更新关联材料的 isIndexed 状态
        if (success > 0) {
            Set<String> materialIds = pendingChunks.stream()
                .map(MaterialChunk::getMaterialId).collect(Collectors.toSet());
            for (String mid : materialIds) {
                Query checkQuery = new Query();
                checkQuery.addCriteria(Criteria.where("materialId").is(mid));
                checkQuery.addCriteria(Criteria.where("indexStatus").is("PENDING"));
                if (!mongoTemplate.exists(checkQuery, MaterialChunk.class)) {
                    Query matQuery = new Query(Criteria.where("id").is(mid));
                    Material mat = mongoTemplate.findOne(matQuery, Material.class);
                    if (mat != null) {
                        mat.setIsIndexed(true);
                        mongoTemplate.save(mat);
                    }
                }
            }
        }

        // 同步所有chunk的materialTitle（修复原始上传时标题为空或占位符的情况）
        syncChunkTitles();

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("failed", failed);
        result.put("total", pendingChunks.size());
        return result;
    }

    /**
     * 从 Material 文档同步 materialTitle 到其下的所有 MaterialChunk（修复标题不一致问题）
     */
    public int syncChunkTitles() {
        int updated = 0;
        List<Material> allMaterials = mongoTemplate.findAll(Material.class);
        for (Material mat : allMaterials) {
            if (mat.getId() == null || mat.getTitle() == null) continue;
            Query chunkQuery = new Query(Criteria.where("materialId").is(mat.getId()));
            List<MaterialChunk> chunks = mongoTemplate.find(chunkQuery, MaterialChunk.class);
            for (MaterialChunk chunk : chunks) {
                if (!mat.getTitle().equals(chunk.getMaterialTitle())) {
                    chunk.setMaterialTitle(mat.getTitle());
                    mongoTemplate.save(chunk);
                    updated++;
                }
            }
        }
        return updated;
    }

    private List<MaterialChunk> splitText(String text, String materialId, String materialTitle) {
        List<MaterialChunk> chunks = new ArrayList<>();
        
        String[] paragraphs = text.split("\n\n+");
        StringBuilder currentChunk = new StringBuilder();
        int currentPosition = 0;
        int chunkIndex = 0;
        
        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) continue;
            
            if (currentChunk.length() + paragraph.length() > CHUNK_SIZE && currentChunk.length() > 0) {
                MaterialChunk chunk = createChunk(materialId, materialTitle, currentChunk.toString(), chunkIndex, currentPosition);
                chunks.add(chunk);
                
                String overlapText = getOverlapText(currentChunk.toString());
                currentChunk = new StringBuilder(overlapText);
                currentPosition += currentChunk.length();
                chunkIndex++;
            }
            
            if (currentChunk.length() > 0) {
                currentChunk.append("\n\n");
            }
            currentChunk.append(paragraph);
        }
        
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
     * 生成文本的向量嵌入。
     *
     * <p>改为调用本地 embedding 服务（bge-small-zh-v1.5，512 维）。
     * 失败时抛出异常而不再造假向量，由调用方决定降级策略。</p>
     */
    public List<Float> generateEmbedding(String text) throws Exception {
        return embeddingClient.embed(text);
    }
    
    /**
     * 混合检索：向量 + BM25，RRF融合
     */
    public List<MaterialChunk> retrieveRelevantChunks(String query, int topK) {
        try {
            // 1. 向量检索：只查 READY 状态、有 embedding 且由当前模型生成的块
            //    （必须按 embeddingModel 过滤：换模型后旧向量维度不同，混入会污染相似度计算）
            List<Float> queryEmbedding = generateEmbedding(query);
            
            Query dbQuery = new Query();
            dbQuery.addCriteria(Criteria.where("indexStatus").is("READY"));
            dbQuery.addCriteria(Criteria.where("embedding").exists(true));
            dbQuery.addCriteria(Criteria.where("embeddingModel").is(EMBEDDING_MODEL_NAME));
            List<MaterialChunk> allChunks = mongoTemplate.find(dbQuery, MaterialChunk.class);
            
            List<MaterialChunk> vectorResults = new ArrayList<>();
            if (!allChunks.isEmpty()) {
                vectorResults = allChunks.stream()
                    .map(chunk -> {
                        float similarity = cosineSimilarity(queryEmbedding, chunk.getEmbedding());
                        if (chunk.getMetadata() == null) chunk.setMetadata(new HashMap<>());
                        chunk.getMetadata().put("score", similarity);
                        return chunk;
                    })
                    .sorted((a, b) -> {
                        Float sa = (Float) a.getMetadata().get("score");
                        Float sb = (Float) b.getMetadata().get("score");
                        return sb.compareTo(sa);
                    })
                    .limit(HYBRID_RECALL_SIZE)
                    .collect(Collectors.toList());
            }
            
            // 2. BM25 关键词召回
            List<MaterialChunk> bm25Results = bm25Search(query, HYBRID_RECALL_SIZE);
            
            // 3. RRF 融合
            if (!bm25Results.isEmpty()) {
                List<MaterialChunk> fused = rrfFusion(vectorResults, bm25Results, topK > 0 ? topK : TOP_K);
                if (!fused.isEmpty()) return fused;
            }
            
            // BM25无结果时降级为纯向量结果
            return vectorResults.stream()
                .limit(topK > 0 ? topK : TOP_K)
                .collect(Collectors.toList());
            
        } catch (Exception e) {
            log.warn("[RAG] 向量检索异常，降级为纯BM25: " + e.getMessage());
            return bm25Search(query, topK > 0 ? topK : TOP_K);
        }
    }
    
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
     * 使用知识库增强的问答——返回结构化结果 {answer, citations[]}
     */
    public Map<String, Object> enhancedQA(String question, String defaultResponse) {
        Map<String, Object> result = new HashMap<>();
        
        List<MaterialChunk> relevantChunks = retrieveRelevantChunks(question, TOP_K);
        
        if (relevantChunks.isEmpty()) {
            result.put("answer", defaultResponse);
            result.put("citations", Collections.emptyList());
            return result;
        }
        
        // 构建citations数组
        List<Map<String, Object>> citations = new ArrayList<>();
        for (MaterialChunk chunk : relevantChunks) {
            Map<String, Object> citation = new HashMap<>();
            citation.put("materialTitle", chunk.getMaterialTitle());
            citation.put("chunkIndex", chunk.getChunkIndex());
            Object scoreObj = chunk.getMetadata() != null ? chunk.getMetadata().get("rrfScore") : null;
            if (scoreObj == null && chunk.getMetadata() != null) {
                scoreObj = chunk.getMetadata().get("score");
            }
            citation.put("score", scoreObj instanceof Float ? (Float) scoreObj : 0f);
            citations.add(citation);
        }
        result.put("citations", citations);
        
        // 构建上下文prompt
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
        
        try {
            String answer = glmApiService.callGlmApi(context.toString());
            result.put("answer", answer);
        } catch (Exception e) {
            log.warn("GLM API调用失败: " + e.getMessage());
            result.put("answer", defaultResponse);
        }
        
        return result;
    }
    
    /**
     * 按章节生成完整学习内容。
     *
     * <p>与 {@link #enhancedQA} 的区别：这里不使用 top-K 语义检索，而是直接按
     * {@code chapterNumber} 拉取该章的<b>全部</b>已索引片段，按顺序拼成上下文。
     * 生成"整章学习内容"属于覆盖面要求极高的任务，top-K 检索只会命中少数片段，
     * 会让模型误判"资料里没有这一章"。</p>
     */
    public Map<String, Object> generateChapterStudyContent(Integer chapter) {
        Map<String, Object> result = new HashMap<>();

        // 1. 该章节的全部资料
        List<Material> materials = getMaterialsByChapter(chapter);
        result.put("materialCount", materials.size());
        if (materials.isEmpty()) {
            result.put("answer", "");
            result.put("citations", Collections.emptyList());
            return result;
        }

        List<String> materialIds = materials.stream()
                .map(Material::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 2. 该章节的全部片段（按资料 + 片段序号排序）
        Query chunkQuery = new Query();
        chunkQuery.addCriteria(Criteria.where("materialId").in(materialIds));
        chunkQuery.addCriteria(Criteria.where("indexStatus").is("READY"));
        List<MaterialChunk> chunks = mongoTemplate.find(chunkQuery, MaterialChunk.class);
        chunks.sort(Comparator
                .comparing((MaterialChunk c) -> c.getMaterialId() == null ? "" : c.getMaterialId())
                .thenComparing(c -> c.getChunkIndex() == null ? 0 : c.getChunkIndex()));

        if (chunks.isEmpty()) {
            result.put("answer", "");
            result.put("citations", Collections.emptyList());
            result.put("chunkCount", 0);
            return result;
        }

        // 3. 全量拼装上下文
        StringBuilder context = new StringBuilder();
        context.append("以下是《电磁场与电磁波（第4版）》第").append(chapter)
               .append("章的教材内容片段，按原文顺序排列：\n\n");

        List<Map<String, Object>> citations = new ArrayList<>();
        int used = 0;
        for (int i = 0; i < chunks.size(); i++) {
            MaterialChunk chunk = chunks.get(i);
            String text = chunk.getContent() == null ? "" : chunk.getContent();
            if (text.isBlank()) {
                continue;
            }
            String block = "【片段" + (i + 1) + "｜" + chunk.getMaterialTitle() + "】\n"
                    + text + "\n\n";
            if (used + block.length() > CHAPTER_CONTEXT_MAX_CHARS) {
                break;
            }
            context.append(block);
            used += block.length();

            Map<String, Object> citation = new HashMap<>();
            citation.put("materialTitle", chunk.getMaterialTitle());
            citation.put("chunkIndex", chunk.getChunkIndex());
            citation.put("score", 1.0f);
            citations.add(citation);
        }
        result.put("citations", citations);
        result.put("chunkCount", citations.size());

        // 4. 明确告知本章资料清单，避免模型因上下文缺失而误判"没有这一章"
        String titles = materials.stream()
                .map(Material::getTitle)
                .collect(Collectors.joining("、"));
        context.append("=====================\n");
        context.append("说明：知识库中第").append(chapter).append("章共有 ")
               .append(materials.size()).append(" 份资料（").append(titles).append("），")
               .append("上面已按顺序给出该章的 ").append(citations.size())
               .append(" 个内容片段，覆盖该章内容，请勿声称\"未找到本章资料\"。\n\n");
        context.append("请基于上述资料，生成第").append(chapter).append("章的完整学习内容，包含：\n")
               .append("1. 章节概述\n")
               .append("2. 核心知识点\n")
               .append("3. 重点难点\n")
               .append("4. 学习建议\n")
               .append("5. 相关习题\n\n")
               .append("要求：所有内容必须来源于上述资料，使用 Markdown 标题与列表组织，条理清晰。");

        try {
            String answer = llmService.chatStrict(context.toString(),
                    LlmCallOptions.defaults()
                            .withTemperature(0.5)
                            .withMaxTokens(CHAPTER_GENERATE_MAX_TOKENS)
                            .withTimeout(Duration.ofSeconds(180)));
            result.put("answer", answer == null ? "" : answer.trim());
        } catch (Exception e) {
            log.warn("[RAG] 按章生成失败: " + e.getMessage());
            result.put("answer", "");
            result.put("error", e.getMessage());
        }
        return result;
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
        query.fields().exclude("chunks");
        return mongoTemplate.find(query, Material.class);
    }
    
    /**
     * 删除资料及其关联的块
     */
    public void deleteMaterial(String materialId) {
        Query chunkQuery = new Query(Criteria.where("materialId").is(materialId));
        List<MaterialChunk> toDelete = mongoTemplate.find(chunkQuery, MaterialChunk.class);
        for (MaterialChunk chunk : toDelete) {
            if (chunk.getId() != null) {
                removeFromBM25Index(chunk.getId());
            }
        }
        mongoTemplate.remove(chunkQuery, MaterialChunk.class);
        
        Query materialQuery = new Query(Criteria.where("id").is(materialId));
        mongoTemplate.remove(materialQuery, Material.class);
    }
    
    /**
     * 重建BM25索引（管理后台手动触发）
     */
    public Map<String, Object> rebuildBM25Index() {
        invertedIndex.clear();
        docLengths.clear();
        totalDocs = 0;
        avgDocLength = 0;
        initBM25Index();
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDocs", totalDocs);
        stats.put("uniqueTerms", invertedIndex.size());
        stats.put("avgDocLength", avgDocLength);
        return stats;
    }
    
    private int estimateTokenCount(String text) {
        return text.length() / 4;
    }
    
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
        long readyCount = mongoTemplate.count(new Query(Criteria.where("indexStatus").is("READY")), MaterialChunk.class);
        long pendingCount = mongoTemplate.count(new Query(Criteria.where("indexStatus").is("PENDING")), MaterialChunk.class);
        
        stats.put("totalMaterials", materialCount);
        stats.put("totalChunks", chunkCount);
        stats.put("readyChunks", readyCount);
        stats.put("pendingChunks", pendingCount);
        stats.put("averageChunksPerMaterial", materialCount > 0 ? (double) chunkCount / materialCount : 0);
        stats.put("bm25UniqueTerms", invertedIndex.size());
        stats.put("bm25TotalDocs", totalDocs);
        
        return stats;
    }
}
