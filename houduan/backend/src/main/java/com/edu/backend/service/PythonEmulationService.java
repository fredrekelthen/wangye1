package com.edu.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 电磁仿真服务调用客户端（Python 实现）。
 *
 * <p>仿真算法已由 MATLAB 全量迁移为 Python 实现，作为独立服务运行
 * （见 {@code qigger/python-emulation}，启动脚本为 {@code start.bat}），
 * 对外暴露与 qigger 一致的 {@code /student/*} 接口。本类通过 HTTP 调用该
 * Python 服务完成计算，并把返回的报文解析为 JSON 树。</p>
 */
@Slf4j
@Service
public class PythonEmulationService {

    /** Python 仿真服务地址，例如 http://localhost:8081 */
    @Value("${emulation.python.base-url:http://localhost:8081}")
    private String baseUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PythonEmulationService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // 建立连接超时 10s
        factory.setConnectTimeout(10_000);
        // 读取超时 180s：启发式优化算法（GA/PSO/SA/Tabu）需数十秒计算，必须留足余量
        factory.setReadTimeout(180_000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * 统一 GET 调用 Python 仿真接口，返回解析后的 JSON。
     *
     * @param path    接口路径，例如 /student/qzs
     * @param builder 已带查询参数的 URI 构造器
     */
    private JsonNode get(String path, UriComponentsBuilder builder) {
        String url = builder.build().encode().toUriString();
        log.info("[电磁仿真] 请求：{}", url);
        long start = System.currentTimeMillis();
        try {
            ResponseEntity<String> response =
                    restTemplate.exchange(url, HttpMethod.GET, null, String.class);
            long cost = System.currentTimeMillis() - start;
            log.info("[电磁仿真] 完成，耗时 {} ms，状态码 {}", cost, response.getStatusCode());
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IllegalStateException("仿真服务返回异常状态：" + response.getStatusCode());
            }
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            log.error("[电磁仿真] 调用失败：{}", e.getMessage());
            throw new RuntimeException("电磁仿真服务调用失败：" + e.getMessage(), e);
        }
    }

    private UriComponentsBuilder uri(String path) {
        return UriComponentsBuilder.fromHttpUrl(baseUrl).path(path);
    }

    /**
     * 双极子天线方向图仿真（GA / PSO / SA / Tabu）。
     *
     * @param state 算法与阵元类型：1=GA_单 2=PSO_单 3=SA_单 4=Tabu_单
     *              5=GA_偶 6=PSO_偶 7=SA_偶 8=Tabu_偶
     */
    public JsonNode doublet(String nu, String value1, String value2, String state) {
        return get("/student/doubletlist", uri("/student/doubletlist")
                .queryParam("nu", nu)
                .queryParam("value1", value1)
                .queryParam("value2", value2)
                .queryParam("state", state));
    }

    /**
     * 单极子天线仿真（旋转方向图）。
     */
    public JsonNode monopole(String nu, String value1, String value2) {
        return get("/student/monopolelist", uri("/student/monopolelist")
                .queryParam("nu", nu)
                .queryParam("value1", value1)
                .queryParam("value2", value2));
    }

    /**
     * 综合仿真：同一组参数依次运行 GA / PSO / SA / Tabu 并对比。
     *
     * @param nu     阵元数
     * @param value1 目标方向角（度）
     * @param kind   阵元类型：sp=单阵元，dp=偶阵元
     */
    public JsonNode emulation(String nu, String value1, String kind) {
        return get("/student/emulationlist", uri("/student/emulationlist")
                .queryParam("nu", nu)
                .queryParam("value1", value1)
                .queryParam("kind", kind));
    }

    /**
     * 入射波 / 反射仿真。
     */
    public JsonNode ybfs(String nu, String value1, String value2) {
        return get("/student/ybfs", uri("/student/ybfs")
                .queryParam("nu", nu)
                .queryParam("value1", value1)
                .queryParam("value2", value2));
    }

    /**
     * 全折射仿真，返回两个结果值。
     */
    public JsonNode qzs(String nu, String value1) {
        return get("/student/qzs", uri("/student/qzs")
                .queryParam("nu", nu)
                .queryParam("value1", value1));
    }

    /**
     * 全反射仿真。
     */
    public JsonNode qfs(String nu) {
        return get("/student/qfs", uri("/student/qfs")
                .queryParam("nu", nu));
    }

    /**
     * 高斯波束仿真。
     */
    public JsonNode gs(String nu, String value1, String value2) {
        return get("/student/gs", uri("/student/gs")
                .queryParam("nu", nu)
                .queryParam("value1", value1)
                .queryParam("value2", value2));
    }

    /**
     * 正弦 EH 场仿真。
     */
    public JsonNode zxb(String value1, String value2) {
        return get("/student/zxb", uri("/student/zxb")
                .queryParam("value1", value1)
                .queryParam("value2", value2));
    }
}
