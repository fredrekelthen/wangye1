package com.edu.backend.controller;

import com.edu.backend.model.dto.UserQueryRequest;
import com.edu.backend.model.entity.CourseStructure;
import com.edu.backend.model.entity.User;
import com.edu.backend.repository.CourseRepository;
import com.edu.backend.repository.UserRepository;
import com.edu.backend.service.AdminService;
import com.edu.backend.utils.BaseResponse;
import com.edu.backend.utils.ResultUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Resource
    private AdminService adminService;

    @Resource
    private UserRepository userRepository;

    @Resource
    private CourseRepository courseRepository;

    @Resource
    private MongoTemplate mongoTemplate;

    /**
     * 检查管理员权限
     */
    private void checkAdminPermission(String userId) {
        if (userId == null || userId.isEmpty()) {
            throw new RuntimeException("用户未登录，请先登录");
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        // 兼容 role 和 userRole 两种字段
        String role = user.getRole();
        String userRole = user.getUserRole();
        boolean isAdmin = "ADMIN".equals(role) || "admin".equals(userRole) || "ADMIN".equals(userRole);
        if (!isAdmin) {
            throw new RuntimeException("无权限访问，仅限管理员操作");
        }
    }

    /**
     * 获取所有用户列表（管理员专用）
     */
    @GetMapping("/users")
    public BaseResponse<Object> getAllUsers(UserQueryRequest userQueryRequest, HttpServletRequest request) {
        try {
            String userId = (String) request.getSession().getAttribute("userId");
            // 检查管理员权限
            checkAdminPermission(userId);

            Object userList = adminService.getAllUsers(userQueryRequest);
            return ResultUtils.success(userList);
        } catch (Exception e) {
            log.error("获取用户列表失败：{}", e.getMessage());
            return ResultUtils.error(40000, e.getMessage());
        }
    }

    /**
     * 获取所有资料列表，按章节分组（管理员专用）
     */
    @GetMapping("/materials-grouped")
    public BaseResponse<Object> getAllMaterialsGroupedByChapter(
            HttpServletRequest request
    ) {
        try {
            String userId = (String) request.getSession().getAttribute("userId");
            // 检查管理员权限
            checkAdminPermission(userId);

            // 直接返回课程结构数据
            List<CourseStructure> courses = courseRepository.findAll(Sort.by(Sort.Direction.ASC, "createTime"));

            if (courses.isEmpty()) {
                // 如果没有课程数据，返回空结果
                Map<String, Object> result = new HashMap<>();
                result.put("chapters", List.of());
                result.put("courseName", "暂无课程数据");
                return ResultUtils.success(result);
            }

            CourseStructure course = courses.get(0);

            Map<String, Object> result = new HashMap<>();
            result.put("chapters", course.getChapters() != null ? course.getChapters() : List.of());
            result.put("courseName", course.getCourseName());
            result.put("description", course.getDescription());

            return ResultUtils.success(result);
        } catch (Exception e) {
            log.error("获取资料列表失败：{}", e.getMessage());
            return ResultUtils.error(40000, e.getMessage());
        }
    }

    /**
     * 更新用户状态（封禁/解封）
     */
    @PutMapping("/user/{userId}/status")
    public BaseResponse<Boolean> updateUserStatus(
            @PathVariable String userId,
            @RequestBody Map<String, Integer> body,
            HttpServletRequest request
    ) {
        try {
            String adminId = (String) request.getSession().getAttribute("userId");
            checkAdminPermission(adminId);

            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return ResultUtils.error(40400, "用户不存在");
            }

            Integer status = body.getOrDefault("userStatus", 0);
            user.setUserStatus(status);
            user.setUpdateTime(new java.util.Date());
            userRepository.save(user);

            log.info("管理员 {} 更新用户 {} 状态为 {}", adminId, userId, status);
            return ResultUtils.success(true, status == 0 ? "解封成功" : "封禁成功");
        } catch (Exception e) {
            log.error("更新用户状态失败：{}", e.getMessage());
            return ResultUtils.error(40000, e.getMessage());
        }
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/user/{userId}")
    public BaseResponse<Boolean> deleteUser(@PathVariable String userId, HttpServletRequest request) {
        try {
            String adminId = (String) request.getSession().getAttribute("userId");
            checkAdminPermission(adminId);

            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return ResultUtils.error(40400, "用户不存在");
            }

            // 不允许删除管理员账号
            if ("ADMIN".equals(user.getRole())) {
                return ResultUtils.error(40001, "不能删除管理员账号");
            }

            // 不能删除自己
            if (userId.equals(adminId)) {
                return ResultUtils.error(40002, "不能删除自己的账号");
            }

            userRepository.deleteById(userId);
            log.info("管理员 {} 删除了用户 {}", adminId, userId);
            return ResultUtils.success(true, "删除成功");
        } catch (Exception e) {
            log.error("删除用户失败：{}", e.getMessage());
            return ResultUtils.error(40000, e.getMessage());
        }
    }

    /**
     * 添加资料到对应的学习内容
     */
    @PostMapping("/material")
    public BaseResponse<Boolean> addMaterial(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request
    ) {
        try {
            String adminId = (String) request.getSession().getAttribute("userId");
            checkAdminPermission(adminId);

            int chapterIndex = (Integer) body.getOrDefault("chapterIndex", 0);
            int sectionIndex = (Integer) body.getOrDefault("sectionIndex", 0);
            String title = (String) body.get("title");
            String type = (String) body.getOrDefault("type", "text");
            String content = (String) body.get("content");
            String description = (String) body.get("description");

            if (title == null || title.isEmpty()) {
                return ResultUtils.error(40000, "资料标题不能为空");
            }
            if (content == null || content.isEmpty()) {
                return ResultUtils.error(40000, "资料内容不能为空");
            }

            // 获取课程结构
            List<CourseStructure> courses = courseRepository.findAll(Sort.by(Sort.Direction.ASC, "createTime"));
            if (courses.isEmpty()) {
                return ResultUtils.error(40400, "课程不存在，请先创建课程");
            }

            CourseStructure course = courses.get(0);

            // 验证章节和小节索引
            if (course.getChapters() == null || course.getChapters().size() <= chapterIndex) {
                return ResultUtils.error(40000, "章节索引无效");
            }

            CourseStructure.CourseChapter chapter = course.getChapters().get(chapterIndex);
            if (chapter.getSections() == null || chapter.getSections().size() <= sectionIndex) {
                return ResultUtils.error(40000, "小节索引无效");
            }

            CourseStructure.CourseSection section = chapter.getSections().get(sectionIndex);

            // 创建新的资料对象（子小节）
            CourseStructure.CourseSubsection newMaterial = new CourseStructure.CourseSubsection();
            newMaterial.setId(java.util.UUID.randomUUID().toString());
            newMaterial.setTitle(title);
            newMaterial.setDescription(description != null ? description : "");
            newMaterial.setStatus("UNREAD");
            newMaterial.setProgress(0);

            // 如果 subsections 为空则初始化
            if (section.getSubsections() == null) {
                section.setSubsections(new ArrayList<>());
            }
            section.getSubsections().add(newMaterial);

            // 更新课程结构的时间戳
            course.setUpdateTime(new java.util.Date());
            courseRepository.save(course);

            log.info("管理员 {} 在章节 {} 小节 {} 添加了新资料: {}", adminId, chapterIndex, sectionIndex, title);
            return ResultUtils.success(true, "添加成功");

        } catch (Exception e) {
            log.error("添加资料失败：{}", e.getMessage());
            return ResultUtils.error(40000, e.getMessage());
        }
    }

    /**
     * 删除资料
     */
    @DeleteMapping("/material/{materialId}")
    public BaseResponse<Boolean> deleteMaterial(@PathVariable String materialId, HttpServletRequest request) {
        try {
            String adminId = (String) request.getSession().getAttribute("userId");
            checkAdminPermission(adminId);

            List<CourseStructure> courses = courseRepository.findAll(Sort.by(Sort.Direction.ASC, "createTime"));
            boolean deleted = false;

            for (CourseStructure course : courses) {
                if (course.getChapters() != null) {
                    for (CourseStructure.CourseChapter chapter : course.getChapters()) {
                        if (chapter.getSections() != null) {
                            for (CourseStructure.CourseSection section : chapter.getSections()) {
                                if (section.getSubsections() != null) {
                                    section.getSubsections().removeIf(material -> materialId.equals(material.getId()));
                                    if (!deleted && section.getSubsections().size() > 0) {
                                        deleted = true;
                                    }
                                }
                            }
                        }
                    }
                }
                courseRepository.save(course);
            }

            log.info("管理员 {} 删除了资料 {}", adminId, materialId);
            return ResultUtils.success(true, "删除成功");

        } catch (Exception e) {
            log.error("删除资料失败：{}", e.getMessage());
            return ResultUtils.error(40000, e.getMessage());
        }
    }

    /**
     * 获取系统统计信息
     */
    @GetMapping("/statistics")
    public BaseResponse<Map<String, Object>> getStatistics(HttpServletRequest request) {
        try {
            String adminId = (String) request.getSession().getAttribute("userId");
            checkAdminPermission(adminId);

            long totalUsers = userRepository.count();
            long studentCount = mongoTemplate.count(
                    Query.query(Criteria.where("role").is("USER")), User.class);
            long adminCount = mongoTemplate.count(
                    Query.query(Criteria.where("role").is("ADMIN")), User.class);
            long activeUsers = mongoTemplate.count(
                    Query.query(Criteria.where("userStatus").is(0)), User.class);
            long bannedUsers = mongoTemplate.count(
                    Query.query(Criteria.where("userStatus").is(1)), User.class);

            List<CourseStructure> courses = courseRepository.findAll();
            int totalChapters = 0;
            int totalSections = 0;
            for (CourseStructure course : courses) {
                if (course.getChapters() != null) {
                    totalChapters += course.getChapters().size();
                    for (CourseStructure.CourseChapter chapter : course.getChapters()) {
                        if (chapter.getSections() != null) {
                            totalSections += chapter.getSections().size();
                        }
                    }
                }
            }

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalUsers", totalUsers);
            stats.put("studentCount", studentCount);
            stats.put("adminCount", adminCount);
            stats.put("activeUsers", activeUsers);
            stats.put("bannedUsers", bannedUsers);
            stats.put("totalCourses", courses.size());
            stats.put("totalChapters", totalChapters);
            stats.put("totalSections", totalSections);

            return ResultUtils.success(stats);
        } catch (Exception e) {
            log.error("获取统计信息失败：{}", e.getMessage());
            return ResultUtils.error(40000, e.getMessage());
        }
    }
}
