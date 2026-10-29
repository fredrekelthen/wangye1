package com.edu.learningagent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek 对话服务（替代原 {@code GlmApiService}）。
 *
 * <p>协议与 deepseek-harness 对齐：凭据来自环境变量 {@code DEEPSEEK_API_KEY}，
 * 端点 {@code https://api.deepseek.com}，默认使用成本最低的 {@code deepseek-flash}。</p>
 *
 * <p>与旧实现的关键差别：调用失败时<b>抛出异常</b>，不再把
 * {@code "与GLM API通信时发生错误: ..."} 这类文本当作正常回复返回
 * （那种文本会被前端当作 AI 答案展示并可能被持久化）。</p>
 */
@Service
public class DeepSeekApiService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekApiService.class);

    private static final String SYSTEM_PROMPT =
            "你是一个专业的电磁场智能学习助手，专注于帮助用户理解项目结构、代码逻辑和业务功能。你应该：\n" +
            "1. 清晰解释项目的架构和技术栈\n" +
            "2. 分析和说明组件的功能和用法\n" +
            "3. 解释数据库设计和数据关系\n" +
            "4. 提供代码优化的建议\n" +
            "5. 帮助理解业务流程\n\n" +
            "回答要简洁、专业、易于理解。";

    @Value("${llm.deepseek.api-key:${DEEPSEEK_API_KEY:}}")
    private String apiKey;

    @Value("${llm.deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    /** 成本最低的模型；复杂任务可切换为 deepseek-v4-pro */
    @Value("${llm.deepseek.model:deepseek-flash}")
    private String model;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DeepSeekApiService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(120_000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * 单轮对话。
     *
     * @param prompt 用户输入
     * @return 模型回答
     * @throws RuntimeException 缺少凭据、网络异常或响应异常时抛出
     */
    public String chat(String prompt) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        messages.add(Map.of("role", "user", "content", prompt == null ? "" : prompt));
        return call(messages, 0.7, 2000);
    }

    /**
     * 携带项目上下文的对话。
     */
    public Map<String, Object> chatWithContext(String prompt, Map<String, Object> context) {
        Map<String, Object> result = new HashMap<>();

        StringBuilder fullPrompt = new StringBuilder(prompt == null ? "" : prompt);
        fullPrompt.append("\n\n项目上下文信息:\n");

        if (context != null) {
            appendContext(fullPrompt, context, "type", "问题类型");
            appendContext(fullPrompt, context, "projectPath", "项目路径");
            appendContext(fullPrompt, context, "routes", "可用路由");
            appendContext(fullPrompt, context, "components", "组件信息");
            appendContext(fullPrompt, context, "database", "数据库信息");
        }

        try {
            result.put("response", chat(fullPrompt.toString()));
            result.put("success", true);
        } catch (RuntimeException e) {
            result.put("error", e.getMessage());
            result.put("success", false);
        }
        return result;
    }

    /** 探活：凭据与端点是否可用。 */
    public boolean validateApiKey() {
        try {
            call(Arrays.asList(Map.of("role", "user", "content", "Hello")), 0.0, 16);
            return true;
        } catch (RuntimeException e) {
            log.warn("DeepSeek 凭据校验失败：{}", e.getMessage());
            return false;
        }
    }

    // ==================== 内部实现 ====================

    private String call(List<Map<String, String>> messages, double temperature, int maxTokens) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "未配置 DeepSeek API Key，请设置环境变量 DEEPSEEK_API_KEY");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", temperature);
        body.put("max_tokens", maxTokens);
        body.put("stream", false);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        String url = baseUrl.replaceAll("/+$", "") + "/chat/completions";

        ResponseEntity<String> resp;
        try {
            resp = restTemplate.exchange(url, HttpMethod.POST,
                    new HttpEntity<>(body, headers), String.class);
        } catch (HttpStatusCodeException e) {
            throw new RuntimeException("DeepSeek 返回 HTTP " + e.getStatusCode().value()
                    + "：" + brief(e.getResponseBodyAsString()), e);
        } catch (ResourceAccessException e) {
            if (e.getCause() instanceof SocketTimeoutException) {
                throw new RuntimeException("DeepSeek 请求超时，请稍后重试", e);
            }
            throw new RuntimeException("DeepSeek 网络异常：" + e.getMessage(), e);
        }

        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new RuntimeException("DeepSeek 返回异常状态：" + resp.getStatusCode());
        }

        try {
            JsonNode root = objectMapper.readTree(resp.getBody());
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                String err = root.path("error").path("message").asText("");
                throw new RuntimeException(err.isEmpty() ? "DeepSeek 响应缺少 choices 字段" : err);
            }
            JsonNode content = choices.get(0).path("message").path("content");
            if (content.isMissingNode() || content.isNull()) {
                throw new RuntimeException("DeepSeek 响应缺少 content 字段");
            }
            if (choices.get(0).path("finish_reason").asText("").equalsIgnoreCase("length")) {
                log.warn("DeepSeek 输出被 max_tokens 截断");
            }
            return content.asText();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("DeepSeek 响应解析失败：" + e.getMessage(), e);
        }
    }

    private void appendContext(StringBuilder sb, Map<String, Object> context,
                               String key, String label) {
        if (context.containsKey(key)) {
            sb.append("- ").append(label).append(": ").append(context.get(key)).append("\n");
        }
    }

    private String brief(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > 200 ? text.substring(0, 200) : text;
    }
}
