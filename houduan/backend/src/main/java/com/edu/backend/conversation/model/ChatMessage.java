package com.edu.backend.conversation.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.*;

/**
 * 对话中的单条消息。
 * MongoDB Collection: agent_messages_v2
 *
 * 注意：本类与 com.edu.backend.agent.AgentMessage 完全不同。
 * AgentMessage 是多Agent间协作消息，本类是用户↔AI的聊天消息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "agent_messages_v2")
public class ChatMessage {

    @Id
    private String id;

    /**
     * 所属会话ID。
     */
    @Indexed
    private String conversationId;

    /**
     * 冗余字段，便于按用户清理。
     */
    @Indexed
    private String userId;

    /**
     * 消息角色："user" 或 "assistant"。
     */
    private String role;

    /**
     * 消息正文。
     */
    private String content;

    /**
     * RAG 溯源列表（仅 assistant 消息可能有）。
     * 每个引用是一个 Map，含 materialTitle / chunkIndex / score 等字段。
     */
    private List<Map<String, Object>> citations;

    /**
     * 会话内顺序号（0, 1, 2, ...），保证严格排序。
     */
    private Integer sequence;

    @Indexed
    private Date createTime;
}
