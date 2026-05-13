package com.edu.backend.service;

import java.util.Map;

/**
 * 个人中心服务接口
 */
public interface UserCenterService {

    /**
     * 获取个人中心统计数据
     */
    Map<String, Object> getUserCenterStats(String userId);

    /**
     * 获取学习进度数据（按天统计）
     */
    Map<String, Object> getLearningProgressByDay(String userId, int days);

    /**
     * 获取近期查看资料列表
     */
    Object getRecentMaterials(String userId, int limit);

    /**
     * 获取个人成绩列表
     */
    Object getUserGrades(String userId, Integer pageNum, Integer pageSize);
}
