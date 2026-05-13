package com.edu.backend.controller;

import com.edu.backend.model.entity.CourseStructure;
import com.edu.backend.repository.CourseRepository;
import com.edu.backend.utils.BaseResponse;
import com.edu.backend.utils.ResultUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 课程结构控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/course")
public class CourseStructureController {

    @Resource
    private CourseRepository courseRepository;

    /**
     * 获取课程结构
     */
    @GetMapping("/structure")
    public BaseResponse<CourseStructure> getCourseStructure() {
        try {
            // 获取第一个课程结构（目前假设只有一个课程）
            List<CourseStructure> courses = courseRepository.findAll();
            if (courses.isEmpty()) {
                // 如果没有课程数据，返回空结构
                CourseStructure emptyStructure = new CourseStructure();
                emptyStructure.setCourseName("电磁场课程");
                emptyStructure.setDescription("暂无课程数据");
                return ResultUtils.success(emptyStructure);
            }
            return ResultUtils.success(courses.get(0));
        } catch (Exception e) {
            log.error("获取课程结构失败：{}", e.getMessage());
            return ResultUtils.error(40000, e.getMessage());
        }
    }
}
