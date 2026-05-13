package com.edu.backend.repository;

import com.edu.backend.model.entity.UserProgress;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户学习进度数据访问接口
 */
@Repository
public interface UserProgressRepository extends MongoRepository<UserProgress, String> {

    /**
     * 根据用户ID查询所有进度
     */
    List<UserProgress> findByUserId(String userId);

    /**
     * 根据用户ID和小节ID查询进度
     */
    UserProgress findByUserIdAndSubsectionId(String userId, String subsectionId);

    /**
     * 根据用户ID和章节ID查询进度列表
     */
    List<UserProgress> findByUserIdAndChapterId(String userId, String chapterId);

    /**
     * 根据用户ID和状态查询进度
     */
    List<UserProgress> findByUserIdAndStatus(String userId, String status);

    /**
     * 查询用户已完成的小节数量
     */
    @Query(value = "{'userId': ?0, 'status': 'COMPLETED'}", count = true)
    long countCompletedByUserId(String userId);

    /**
     * 查询用户所有小节总数
     */
    @Query(value = "{'userId': ?0}", count = true)
    long countAllByUserId(String userId);
}
