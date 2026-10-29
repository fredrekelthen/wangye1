package com.edu.backend.llm;

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
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek 提供方实现（capability seam 的 Service Provider）。
 *
 * <p>协议对齐 deepseek-harness 的 {@code @deepseek-ai/dsh-llm-deepseek}：
 * <ul>
 *   <li>凭据来自环境变量 {@code DEEPSEEK_API_KEY}</li>
 *   <li>端点 {@code https://api.deepseek.com}（OpenAI 兼容 chat-completions）</li>
 *   <li>模型 id 原样透传，默认使用最省成本的 {@code deepseek-flash}</li>
 * </ul>
 *
 * <p>所有失败形式（缺少凭据、非 2xx、协议成功但缺字段、网络/超时）一律规范化为
 * {@link LlmException}，绝不返回"看起来像答案的错误文本"。</p>
 */
@Slf4j
@Component
public class DeepSeekLlmProvider implements LlmProvider {

    /** 与 harness 的 apiKeyEnv 保持一致 */
    @Value("${llm.deepseek.api-key:${DEEPSEEK_API_KEY:}}")
    private String apiKey;

    @Value("${llm.deepseek.base-url:https://api.deepseek.com}")
    private String baseUrl;

    /** 默认使用成本最低的模型；可切换为 deepseek-v4-pro */
    @Value("${llm.deepseek.model:deepseek-flash}")
    private String model;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DeepSeekLlmProvider() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(120_000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Override
    public String id() {
        return "deepseek";
    }

    @Override
    public LlmCallResult chat(List<LlmMessage> messages, LlmCallOptions options) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new LlmException(LlmException.Kind.AUTH,
                    "未配置 DeepSeek API Key，请设置环境变量 DEEPSEEK_API_KEY");
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", toWireMessages(messages));
        body.put("temperature", options.getTemperature());
        body.put("max_tokens", options.getMaxTokens());
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
            throw normalizeHttpFailure(e);
        } catch (ResourceAccessException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SocketTimeoutException) {
                throw new LlmException(LlmException.Kind.TIMEOUT,
                        "请求超时：" + e.getMessage(), null, e);
            }
            throw new LlmException(LlmException.Kind.TRANSPORT,
                    "网络异常：" + e.getMessage(), null, e);
        } catch (Exception e) {
            throw new LlmException(LlmException.Kind.UNKNOWN,
                    "调用 DeepSeek 失败：" + e.getMessage(), null, e);
        }

        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new LlmException(LlmException.Kind.SERVER,
                    "返回异常状态：" + resp.getStatusCode(),
                    resp.getStatusCode().value(), null);
        }

        return parse(resp.getBody());
    }

    private List<Map<String, String>> toWireMessages(List<LlmMessage> messages) {
        List<Map<String, String>> list = new ArrayList<>();
        if (messages == null) {
            return list;
        }
        for (LlmMessage m : messages) {
            Map<String, String> one = new HashMap<>();
            one.put("role", m.getRole().getWireName());
            one.put("content", m.getContent());
            list.add(one);
        }
        return list;
    }

    private LlmCallResult parse(String rawBody) {
        JsonNode root;
        try {
            root = objectMapper.readTree(rawBody);
        } catch (Exception e) {
            throw new LlmException(LlmException.Kind.BAD_RESPONSE,
                    "响应无法解析为 JSON", null, e);
        }

        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            // DeepSeek 在部分错误场景下以 200 返回 error 字段
            String errMsg = root.path("error").path("message").asText("");
            throw new LlmException(LlmException.Kind.BAD_RESPONSE,
                    errMsg.isEmpty() ? "响应中缺少 choices 字段" : errMsg, null, null);
        }

        JsonNode message = choices.get(0).path("message");
        JsonNode content = message.path("content");
        if (content.isMissingNode() || content.isNull()) {
            throw new LlmException(LlmException.Kind.BAD_RESPONSE,
                    "响应中缺少 content 字段", null, null);
        }

        JsonNode usage = root.path("usage");
        Integer promptTokens = usage.path("prompt_tokens").isNumber()
                ? usage.get("prompt_tokens").asInt() : null;
        Integer completionTokens = usage.path("completion_tokens").isNumber()
                ? usage.get("completion_tokens").asInt() : null;
        Integer totalTokens = usage.path("total_tokens").isNumber()
                ? usage.get("total_tokens").asInt() : null;

        String finishReason = choices.get(0).path("finish_reason").asText(null);
        String modelName = root.path("model").asText(model);

        LlmCallResult result = new LlmCallResult(
                content.asText(), modelName, promptTokens, completionTokens, totalTokens, finishReason);
        if (result.isTruncated()) {
            log.warn("[DeepSeek] 输出被 max_tokens 截断（finish_reason=length）");
        }
        return result;
    }

    private LlmException normalizeHttpFailure(HttpStatusCodeException e) {
        int status = e.getStatusCode().value();
        String detail = e.getResponseBodyAsString();
        if (detail != null && detail.length() > 300) {
            detail = detail.substring(0, 300);
        }

        LlmException.Kind kind = switch (status) {
            case 401, 403 -> LlmException.Kind.AUTH;
            case 429 -> LlmException.Kind.RATE_LIMIT;
            case 400 -> LlmException.Kind.CONTEXT_OVERFLOW;
            default -> status >= 500 ? LlmException.Kind.SERVER : LlmException.Kind.UNKNOWN;
        };
        return new LlmException(kind,
                "DeepSeek 返回 HTTP " + status + (detail == null || detail.isBlank() ? "" : "：" + detail),
                status, e);
    }
}
