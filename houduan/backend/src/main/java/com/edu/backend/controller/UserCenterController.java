package com.edu.backend.controller;

import com.edu.backend.service.UserCenterService;
import com.edu.backend.utils.BaseResponse;
import com.edu.backend.utils.ResultUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 个人中心控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/user-center")
public class UserCenterController {

    @Resource
    private UserCenterService userCenterService;

    /**
     * 获取个人中心统计数据
     */
    @GetMapping("/stats")
    public BaseResponse<Map<String, Object>> getUserCenterStats(HttpServletRequest request) {
        try {
            String userId = (String) request.getSession().getAttribute("userId");
            Map<String, Object> stats = userCenterService.getUserCenterStats(userId);
            return ResultUtils.success(stats);
        } catch (Exception e) {
            log.error("获取个人中心统计失败：{}", e.getMessage());
            return ResultUtils.error(40000, e.getMessage());
        }
    }

    /**
     * 获取学习进度数据（按天统计）
     */
    @GetMapping("/learning-progress")
    public BaseResponse<Map<String, Object>> getLearningProgressByDay(
            HttpServletRequest request,
            @RequestParam(defaultValue = "7") int days
    ) {
        try {
            String userId = (String) request.getSession().getAttribute("userId");
            Map<String, Object> progress = userCenterService.getLearningProgressByDay(userId, days);
            return ResultUtils.success(progress);
        } catch (Exception e) {
            log.error("获取学习进度失败：{}", e.getMessage());
            return ResultUtils.error(40000, e.getMessage());
        }
    }

    /**
     * 获取近期查看资料
     */
    @GetMapping("/recent-materials")
    public BaseResponse<Object> getRecentMaterials(
            HttpServletRequest request,
            @RequestParam(defaultValue = "10") int limit
    ) {
        try {
            String userId = (String) request.getSession().getAttribute("userId");
            Object materials = userCenterService.getRecentMaterials(userId, limit);
            return ResultUtils.success(materials);
        } catch (Exception e) {
            log.error("获取近期资料失败：{}", e.getMessage());
            return ResultUtils.error(40000, e.getMessage());
        }
    }

    /**
     * 获取个人成绩列表
     */
    @GetMapping("/grades")
    public BaseResponse<Object> getUserGrades(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        try {
            String userId = (String) request.getSession().getAttribute("userId");
            Object grades = userCenterService.getUserGrades(userId, pageNum, pageSize);
            return ResultUtils.success(grades);
        } catch (Exception e) {
            log.error("获取个人成绩失败：{}", e.getMessage());
            return ResultUtils.error(40000, e.getMessage());
        }
    }
}
