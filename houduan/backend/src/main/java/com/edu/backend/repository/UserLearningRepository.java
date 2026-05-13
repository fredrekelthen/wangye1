package com.edu.backend.repository;

import com.edu.backend.model.entity.UserLearning;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * 用户学习记录数据访问接口
 */
@Repository
public interface UserLearningRepository extends MongoRepository<UserLearning, String> {

    /**
     * 根据用户ID查询学习记录
     */
    List<UserLearning> findByUserIdOrderByStudyTimeDesc(String userId);

    /**
     * 根据用户ID分页查询学习记录
     */
    Page<UserLearning> findByUserIdOrderByStudyTimeDesc(String userId, Pageable pageable);

    /**
     * 根据用户ID查询最近N条学习记录
     */
    @Query(value = "{'userId': ?0}")
    List<UserLearning> findRecentByUserId(String userId, Pageable pageable);

    /**
     * 查询用户在某时间范围内的学习记录
     */
    List<UserLearning> findByUserIdAndStudyTimeBetween(String userId, Date startTime, Date endTime);

    /**
     * 统计用户学习次数
     */
    @Query(value = "{'userId': ?0}", count = true)
    long countByUserId(String userId);

    /**
     * 统计用户在某时间范围内的学习次数
     */
    @Query(value = "{'userId': ?0, 'studyTime': {$gte: ?1, $lte: ?2}}", count = true)
    long countByUserIdAndStudyTimeBetween(String userId, Date startTime, Date endTime);
}
