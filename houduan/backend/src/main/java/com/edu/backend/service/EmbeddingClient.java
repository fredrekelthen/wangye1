package com.edu.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 本地 Embedding 服务客户端。
 *
 * <p>对话模型切换到 DeepSeek 后，DeepSeek 官方 API 不提供 embeddings 接口
 * （实测 {@code POST /embeddings} 返回 404），因此向量化改为调用本地 embedding 服务：
 * 见 {@code houduan/embedding-service}，模型 {@code BAAI/bge-small-zh-v1.5}（512 维），
 * 零 API 成本、离线可用，且中文语义召回优于原先的纯关键词匹配。</p>
 */
@Slf4j
@Service
public class EmbeddingClient {

    /** 模型标识，写入 chunk 的 embeddingModel 字段，用于区分不同模型产生的向量 */
    public static final String MODEL_NAME = "bge-small-zh-v1.5";

    /** 向量维度 */
    public static final int DIMENSION = 512;

    @Value("${embedding.local.base-url:http://localhost:8000}")
    private String baseUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EmbeddingClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        // 首次调用可能触发模型加载，留足余量
        factory.setReadTimeout(120_000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * 单条文本向量化。
     *
     * @throws Exception 服务不可达、返回异常或向量维度不符
     */
    public List<Float> embed(String text) throws Exception {
        List<List<Float>> vectors = embedBatch(List.of(text == null ? "" : text));
        if (vectors.isEmpty()) {
            throw new Exception("Embedding 服务返回空结果");
        }
        return vectors.get(0);
    }

    /**
     * 批量向量化。
     *
     * @param texts 待向量化文本
     * @return 与输入一一对应的向量列表
     * @throws Exception 调用失败或维度不符
     */
    public List<List<Float>> embedBatch(List<String> texts) throws Exception {
        if (texts == null || texts.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, Object> body = new HashMap<>();
        body.put("texts", texts);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<String> resp = restTemplate.exchange(
                    baseUrl + "/embed", HttpMethod.POST,
                    new HttpEntity<>(body, headers), String.class);

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                throw new IllegalStateException("返回异常状态：" + resp.getStatusCode());
            }

            JsonNode root = objectMapper.readTree(resp.getBody());
            JsonNode arr = root.path("vectors");
            if (!arr.isArray() || arr.isEmpty()) {
                throw new IllegalStateException("返回空向量");
            }

            List<List<Float>> result = new ArrayList<>(arr.size());
            for (JsonNode vecNode : arr) {
                List<Float> vec = new ArrayList<>(vecNode.size());
                for (JsonNode v : vecNode) {
                    vec.add((float) v.asDouble());
                }
                if (vec.size() != DIMENSION) {
                    throw new IllegalStateException(
                            "向量维度不符：期望 " + DIMENSION + "，实际 " + vec.size());
                }
                result.add(vec);
            }
            return result;

        } catch (Exception e) {
            log.error("[Embedding] 调用失败：{}", e.getMessage());
            throw new Exception("本地 Embedding 服务调用失败：" + e.getMessage(), e);
        }
    }

    /** 服务是否可用（供管理接口做健康检查）。 */
    public boolean isAvailable() {
        try {
            ResponseEntity<String> resp = restTemplate.getForEntity(baseUrl + "/health", String.class);
            return resp.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
}
