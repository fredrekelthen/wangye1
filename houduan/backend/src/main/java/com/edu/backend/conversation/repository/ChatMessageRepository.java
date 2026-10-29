package com.edu.backend.conversation.repository;

import com.edu.backend.conversation.model.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 聊天消息持久化接口。
 */
@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    /**
     * 按会话和序号升序获取全部消息（回看历史用）。
     */
    List<ChatMessage> findByConversationIdOrderBySequenceAsc(String conversationId);

    /**
     * 统计会话内消息数。
     */
    long countByConversationId(String conversationId);

    /**
     * 删除会话内全部消息。
     */
    void deleteByConversationId(String conversationId);
}
