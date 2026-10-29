package com.edu.backend.conversation.repository;

import com.edu.backend.conversation.model.Conversation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 会话持久化接口。
 */
@Repository
public interface ConversationRepository extends MongoRepository<Conversation, String> {

    /**
     * 按用户查询所有未删除会话，按最近活动倒序。
     */
    List<Conversation> findByUserIdAndDeletedFalseOrderByUpdateTimeDesc(String userId);

    /**
     * 按ID和用户查询单条会话（用于越权校验）。
     */
    Optional<Conversation> findByIdAndUserId(String id, String userId);
}
