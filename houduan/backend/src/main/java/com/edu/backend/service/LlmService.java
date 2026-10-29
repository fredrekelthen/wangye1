package com.edu.backend.service;

import com.edu.backend.llm.LlmCallOptions;
import com.edu.backend.llm.LlmCallResult;
import com.edu.backend.llm.LlmException;
import com.edu.backend.llm.LlmMessage;
import com.edu.backend.llm.LlmProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 大模型调用门面（capability seam 的 Consumer 侧统一入口）。
 *
 * <p>替代原先的 {@code GlmApiService}。底层提供方由 {@link LlmProvider} 决定，
 * 当前由 {@code DeepSeekLlmProvider} 实现（模型 {@code deepseek-flash}），
 * 更换厂商时本类与所有调用方都无需改动。</p>
 *
 * <p><b>两套语义</b>：
 * <ul>
 *   <li><b>严格方法（{@code *Strict}）</b>：失败抛 {@link LlmException}。
 *       会写入会话历史的路径必须使用，避免把错误文本当成 AI 答案持久化。</li>
 *   <li><b>兼容方法</b>：失败返回统一的降级文案，不抛异常。
 *       供"失败可接受、不影响主流程"的场景使用。</li>
 * </ul>
 */
@Slf4j
@Service
public class LlmService {

    /** 兼容模式下失败时的统一文案：既不泄漏异常细节，也不冒充答案 */
    private static final String FALLBACK_TEXT = "AI 服务暂时不可用，请稍后重试。";

    private static final String DEFAULT_SYSTEM_PROMPT =
            "你是一个简洁的助手。回答要简短直接，不要输出任何项目介绍、项目结构、技术细节等无关内容。";

    private final LlmProvider provider;

    public LlmService(LlmProvider provider) {
        this.provider = provider;
    }

    /** 当前提供方标识，便于日志与排查。 */
    public String providerId() {
        return provider.id();
    }

    // ==================== 严格模式：失败抛 LlmException ====================

    public String chatStrict(String prompt) {
        return chatStrict(prompt, LlmCallOptions.defaults());
    }

    public String chatStrict(String prompt, LlmCallOptions options) {
        List<LlmMessage> messages = List.of(
                LlmMessage.system(DEFAULT_SYSTEM_PROMPT),
                LlmMessage.user(prompt == null ? "" : prompt));
        return requireContent(provider.chat(messages, options));
    }

    public String chatWithHistoryStrict(List<Map<String, String>> messages,
                                        double temperature, int maxTokens) {
        return requireContent(provider.chat(toLlmMessages(messages),
                LlmCallOptions.defaults()
                        .withTemperature(temperature)
                        .withMaxTokens(maxTokens)));
    }

    /**
     * 校验模型结果，拦截"调用成功但内容为空"。
     *
     * <p>推理模型（deepseek-flash）的思考过程与正式回答共享 max_tokens：
     * 额度被思考耗尽时 {@code finish_reason=length} 且 content 为空字符串。
     * 这属于<b>失败</b>而非"空答案"，必须显式抛出，否则空字符串会冒充答案
     * 流入上层（甚至被持久化进会话历史）。</p>
     */
    private String requireContent(LlmCallResult result) {
        String content = result.getContent();
        if ((content == null || content.isBlank()) && result.isTruncated()) {
            throw new LlmException(LlmException.Kind.CONTEXT_OVERFLOW,
                    "模型输出被 max_tokens 截断且内容为空（推理模型的思考过程耗尽了额度），"
                            + "请提高该调用点的 max_tokens");
        }
        return content == null ? "" : content;
    }

    // ==================== 兼容模式：失败返回统一降级文案 ====================

    public String chat(String prompt) {
        return chat(prompt, LlmCallOptions.defaults());
    }

    /** 兼容模式 + 自定义调用选项：失败返回统一降级文案。 */
    public String chat(String prompt, LlmCallOptions options) {
        try {
            return chatStrict(prompt, options);
        } catch (LlmException e) {
            log.warn("[LLM] chat 调用失败：{}（{}）", e.getMessage(), e.getKind());
            return FALLBACK_TEXT;
        }
    }

    public String chatWithHistory(List<Map<String, String>> messages) {
        // 使用统一默认额度（推理模型下 800 会被思考耗光，见 LlmCallOptions）
        return chatWithHistory(messages, 0.7, LlmCallOptions.defaults().getMaxTokens());
    }

    public String chatWithHistory(List<Map<String, String>> messages,
                                  double temperature, int maxTokens) {
        try {
            return chatWithHistoryStrict(messages, temperature, maxTokens);
        } catch (LlmException e) {
            log.warn("[LLM] chatWithHistory 调用失败：{}（{}）", e.getMessage(), e.getKind());
            return FALLBACK_TEXT;
        }
    }

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
            result.put("response", chatStrict(fullPrompt.toString()));
            result.put("success", true);
        } catch (LlmException e) {
            log.warn("[LLM] chatWithContext 调用失败：{}（{}）", e.getMessage(), e.getKind());
            result.put("error", e.getMessage());
            result.put("success", false);
        }
        return result;
    }

    // ==================== 内部工具 ====================

    private void appendContext(StringBuilder sb, Map<String, Object> context,
                               String key, String label) {
        if (context.containsKey(key)) {
            sb.append("- ").append(label).append(": ").append(context.get(key)).append("\n");
        }
    }

    private List<LlmMessage> toLlmMessages(List<Map<String, String>> messages) {
        List<LlmMessage> list = new ArrayList<>();
        if (messages == null) {
            return list;
        }
        for (Map<String, String> m : messages) {
            String role = m.getOrDefault("role", "user");
            String content = m.getOrDefault("content", "");
            switch (role.toLowerCase()) {
                case "system" -> list.add(LlmMessage.system(content));
                case "assistant" -> list.add(LlmMessage.assistant(content));
                default -> list.add(LlmMessage.user(content));
            }
        }
        return list;
    }
}
