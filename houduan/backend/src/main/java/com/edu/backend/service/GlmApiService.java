package com.edu.backend.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 过渡适配器：把既有调用转发给 {@link LlmService}。
 *
 * <p><b>历史</b>：本类原先直接以 HTTP 调用智谱 GLM，并把失败信息当作正常回复返回
 * （{@code return "与GLM API通信时发生错误: ..."}），该字符串会被上层误当作 AI 答案
 * 持久化进会话历史。现已改造为纯转发，失败语义统一由 {@link LlmService} 处理。</p>
 *
 * @deprecated 对话模型已从 GLM 切换为 DeepSeek（{@code deepseek-flash}），
 *             本类仅为兼容既有调用方而保留。新代码请直接注入 {@link LlmService}，
 *             后续应逐步迁移调用方并删除本类。
 */
@Deprecated
@Service
public class GlmApiService {

    private final LlmService llmService;

    public GlmApiService(LlmService llmService) {
        this.llmService = llmService;
    }

    /** @deprecated 改用 {@link LlmService#chat(String)} */
    @Deprecated
    public String chat(String prompt) {
        return llmService.chat(prompt);
    }

    /** @deprecated 改用 {@link LlmService#chatWithContext(String, Map)} */
    @Deprecated
    public Map<String, Object> chatWithContext(String prompt, Map<String, Object> context) {
        return llmService.chatWithContext(prompt, context);
    }

    /** @deprecated 改用 {@link LlmService#chatWithHistory(List)} */
    @Deprecated
    public String chatWithHistory(List<Map<String, String>> messages) {
        return llmService.chatWithHistory(messages);
    }

    /** @deprecated 改用 {@link LlmService#chatWithHistory(List, double, int)} */
    @Deprecated
    public String chatWithHistory(List<Map<String, String>> messages,
                                  double temperature, int maxTokens) {
        return llmService.chatWithHistory(messages, temperature, maxTokens);
    }

    /** @deprecated 改用 {@link LlmService#chat(String)} */
    @Deprecated
    public String callGlmApi(String prompt) {
        return llmService.chat(prompt);
    }
}
