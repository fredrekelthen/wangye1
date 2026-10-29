package com.edu.backend.conversation.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * 用户与AI的对话会话。
 * MongoDB Collection: agent_conversations
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "agent_conversations")
public class Conversation {

    @Id
    private String id;

    /**
     * 归属用户ID，按用户隔离。
     */
    @Indexed
    private String userId;

    /**
     * 会话标题（首条用户消息的前20字，或"新对话"）。
     */
    private String title;

    /**
     * 消息条数（冗余字段，用于列表展示）。
     */
    private Integer messageCount;

    /**
     * 最后一条消息的文本预览（列表展示用）。
     */
    private String lastMessagePreview;

    @Indexed
    private Date createTime;

    /**
     * 每次追加消息时刷新，用于按最近活动排序。
     */
    @Indexed
    private Date updateTime;

    /**
     * 软删除标记。
     */
    private Boolean deleted = false;
}
