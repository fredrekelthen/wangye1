package com.edu.backend.repository;

import com.edu.backend.model.entity.CourseStructure;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 课程结构数据访问接口
 */
@Repository
public interface CourseRepository extends MongoRepository<CourseStructure, String> {

    /**
     * 根据课程名称查询课程结构
     */
    CourseStructure findByCourseName(String courseName);

    /**
     * 根据状态查询课程列表
     */
    List<CourseStructure> findByStatus(String status);
}
