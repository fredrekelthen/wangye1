package com.edu.backend.conversation.service;

import com.edu.backend.conversation.model.ChatMessage;
import com.edu.backend.conversation.model.Conversation;
import com.edu.backend.conversation.repository.ChatMessageRepository;
import com.edu.backend.conversation.repository.ConversationRepository;
import com.edu.backend.llm.LlmCallOptions;
import com.edu.backend.llm.LlmException;
import com.edu.backend.service.LlmService;
import com.edu.backend.service.RAGKnowledgeBaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 对话会话服务 —— 新建会话、发消息（带多轮上下文记忆）、列表、回看、删除。
 */
@Slf4j
@Service
public class ConversationService {

    /** 上下文窗口：每次拼接最近 N 条历史消息 */
    private static final int MAX_HISTORY_MESSAGES = 10;

    /** 会话内 system 角色设定 */
    private static final String SYSTEM_PROMPT =
        "你是一个智能教育助手，专门帮助学习者理解知识、解答疑问。"
        + "回答要清晰准确，引用提供的资料佐证。资料不足时如实说明。";

    /** RAG 触发关键词（命中任一即检索知识库） */
    private static final List<String> RAG_KEYWORDS = List.of(
        "学习", "课程", "知识点", "章节", "内容", "讲解", "解释",
        "什么是", "如何", "为什么", "怎么做", "定义", "公式", "原理",
        "电磁", "矢量", "电场", "磁场", "电磁波", "麦克斯韦", "梯度", "散度", "旋度"
    );

    private final ConversationRepository conversationRepo;
    private final ChatMessageRepository messageRepo;
    private final LlmService llmService;
    private final RAGKnowledgeBaseService ragService;

    public ConversationService(ConversationRepository conversationRepo,
                               ChatMessageRepository messageRepo,
                               LlmService llmService,
                               RAGKnowledgeBaseService ragService) {
        this.conversationRepo = conversationRepo;
        this.messageRepo = messageRepo;
        this.llmService = llmService;
        this.ragService = ragService;
    }

    // ==================== 新建空会话 ====================

    /**
     * 为用户创建一个全新的空会话。
     * @return 会话ID
     */
    public String createConversation(String userId) {
        Date now = new Date();
        Conversation conv = new Conversation();
        conv.setId(UUID.randomUUID().toString());
        conv.setUserId(userId);
        conv.setTitle("新对话");
        conv.setMessageCount(0);
        conv.setLastMessagePreview("");
        conv.setCreateTime(now);
        conv.setUpdateTime(now);
        conv.setDeleted(false);
        conversationRepo.save(conv);
        return conv.getId();
    }

    // ==================== 发消息（核心多轮记忆链路） ====================

    /**
     * 在指定会话中发送一条用户消息，返回 AI 回复（含溯源）。
     */
    public Map<String, Object> sendMessage(String userId, String conversationId, String content) {
        // 0. 校验会话归属
        Conversation conv = conversationRepo.findByIdAndUserId(conversationId, userId)
            .orElseThrow(() -> new RuntimeException("会话不存在或无权访问"));

        // 1. 计算顺序号
        long existingCount = messageRepo.countByConversationId(conversationId);
        int sequence = (int) existingCount;

        Date now = new Date();

        // 2. 存用户消息
        ChatMessage userMsg = new ChatMessage();
        userMsg.setId(UUID.randomUUID().toString());
        userMsg.setConversationId(conversationId);
        userMsg.setUserId(userId);
        userMsg.setRole("user");
        userMsg.setContent(content);
        userMsg.setSequence(sequence);
        userMsg.setCreateTime(now);
        messageRepo.save(userMsg);

        // 3. 读窗口内历史（本会话最近 N 条）
        List<ChatMessage> allMessages = messageRepo.findByConversationIdOrderBySequenceAsc(conversationId);
        List<ChatMessage> windowMessages = allMessages.subList(
            Math.max(0, allMessages.size() - MAX_HISTORY_MESSAGES), allMessages.size());

        // 4. 构造 messages 数组传给 GLM
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));
        for (ChatMessage m : windowMessages) {
            messages.add(Map.of("role", m.getRole(), "content", m.getContent()));
        }

        // 5. RAG 检索增强（按关键词命中）
        String aiAnswer;
        List<Map<String, Object>> citations = new ArrayList<>();
        boolean triggedRag = RAG_KEYWORDS.stream().anyMatch(kw -> content.contains(kw));
        if (triggedRag) {
            Map<String, Object> ragResult = ragService.enhancedQA(content,
                "抱歉，我没有找到相关的学习资料来回答您的问题。");
            String ragAnswer = (String) ragResult.get("answer");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> ragCitations =
                (List<Map<String, Object>>) ragResult.getOrDefault("citations", Collections.emptyList());

            if (ragAnswer != null && !ragAnswer.isEmpty() && !ragAnswer.contains("抱歉")) {
                // 把 RAG 检索结果作为额外 system 消息注入，让 LLM 基于它回答
                messages.add(Map.of("role", "system",
                    "content", "以下是知识库检索到的相关资料，请基于它回答用户问题：\n" + ragAnswer));
                citations = ragCitations;
            }
        }

        // 严格模式：模型调用失败必须抛出，避免把错误文本当作 AI 答案写进会话历史
        try {
            // 使用统一默认额度：推理模型下 800 会被思考过程耗尽（见 LlmCallOptions）
            aiAnswer = llmService.chatWithHistoryStrict(messages, 0.7,
                    LlmCallOptions.defaults().getMaxTokens());
        } catch (LlmException e) {
            log.error("[对话] 模型调用失败：{}（{}）", e.getMessage(), e.getKind());
            throw new RuntimeException(switch (e.getKind()) {
                case AUTH -> "AI 服务未配置或凭据无效，请联系管理员检查 DEEPSEEK_API_KEY";
                case RATE_LIMIT -> "AI 服务当前限流，请稍后重试";
                case TIMEOUT, TRANSPORT -> "AI 服务网络异常，请稍后重试";
                default -> "AI 服务暂时不可用，请稍后重试";
            });
        }

        // 6. 存助手消息
        ChatMessage assistantMsg = new ChatMessage();
        assistantMsg.setId(UUID.randomUUID().toString());
        assistantMsg.setConversationId(conversationId);
        assistantMsg.setUserId(userId);
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(aiAnswer);
        assistantMsg.setCitations(citations.isEmpty() ? null : citations);
        assistantMsg.setSequence(sequence + 1);
        assistantMsg.setCreateTime(new Date());
        messageRepo.save(assistantMsg);

        // 7. 更新会话元数据
        int newCount = (int) (existingCount + 2); // user + assistant
        conv.setMessageCount(newCount);
        conv.setLastMessagePreview(truncate(aiAnswer, 60));
        conv.setUpdateTime(new Date());
        // 首条用户消息自动生成标题
        if (conv.getTitle() == null || "新对话".equals(conv.getTitle())) {
            conv.setTitle(truncate(content, 20));
        }
        conversationRepo.save(conv);

        // 8. 返回
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("conversationId", conversationId);
        response.put("answer", aiAnswer);
        response.put("citations", citations);
        response.put("sequence", sequence + 1);
        return response;
    }

    // ==================== 列表 / 回看 / 删除 / 重命名 ====================

    /**
     * 获取用户的所有会话列表（最近活动倒序）。
     */
    public List<Map<String, Object>> listConversations(String userId) {
        List<Conversation> convs = conversationRepo.findByUserIdAndDeletedFalseOrderByUpdateTimeDesc(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Conversation c : convs) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", c.getId());
            item.put("title", c.getTitle());
            item.put("messageCount", c.getMessageCount());
            item.put("lastMessagePreview", c.getLastMessagePreview());
            item.put("createTime", c.getCreateTime());
            item.put("updateTime", c.getUpdateTime());
            result.add(item);
        }
        return result;
    }

    /**
     * 获取某会话的全部消息（按顺序），校验用户归属。
     */
    public List<ChatMessage> getMessages(String userId, String conversationId) {
        Conversation conv = conversationRepo.findByIdAndUserId(conversationId, userId)
            .orElseThrow(() -> new RuntimeException("会话不存在或无权访问"));
        return messageRepo.findByConversationIdOrderBySequenceAsc(conversationId);
    }

    /**
     * 软删除会话 + 删除关联消息。
     */
    public void deleteConversation(String userId, String conversationId) {
        Conversation conv = conversationRepo.findByIdAndUserId(conversationId, userId)
            .orElseThrow(() -> new RuntimeException("会话不存在或无权访问"));
        conv.setDeleted(true);
        conversationRepo.save(conv);
        messageRepo.deleteByConversationId(conversationId);
    }

    /**
     * 重命名会话标题。
     */
    public void renameConversation(String userId, String conversationId, String title) {
        Conversation conv = conversationRepo.findByIdAndUserId(conversationId, userId)
            .orElseThrow(() -> new RuntimeException("会话不存在或无权访问"));
        conv.setTitle(title != null && !title.isBlank() ? title.trim() : conv.getTitle());
        conversationRepo.save(conv);
    }

    // ==================== 私有工具方法 ====================

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "…";
    }
}
