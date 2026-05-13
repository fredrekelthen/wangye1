package com.edu.backend.repository;

import com.edu.backend.model.entity.UserGrade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户成绩数据访问接口
 */
@Repository
public interface UserGradeRepository extends MongoRepository<UserGrade, String> {

    /**
     * 根据用户ID查询所有成绩
     */
    List<UserGrade> findByUserIdOrderByExamTimeDesc(String userId);

    /**
     * 根据用户ID分页查询成绩
     */
    Page<UserGrade> findByUserIdOrderByExamTimeDesc(String userId, Pageable pageable);

    /**
     * 根据用户ID和考试类型查询成绩
     */
    List<UserGrade> findByUserIdAndExamTypeOrderByExamTimeDesc(String userId, String examType);

    /**
     * 统计用户成绩数量
     */
    @Query(value = "{'userId': ?0}", count = true)
    long countByUserId(String userId);

    /**
     * 计算用户平均分
     */
    @Query(value = "{'userId': ?0}", fields = "{'score': 1}")
    List<UserGrade> findScoresByUserId(String userId);
}
