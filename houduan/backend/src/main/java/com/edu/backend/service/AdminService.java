package com.edu.backend.service;

import com.edu.backend.model.dto.UserQueryRequest;

/**
 * 管理员服务接口
 */
public interface AdminService {

    /**
     * 获取所有用户列表（管理员专用）
     */
    Object getAllUsers(UserQueryRequest userQueryRequest);

    /**
     * 获取所有资料列表，按章节分组（管理员专用）
     */
    Object getAllMaterialsGroupedByChapter(Integer pageNum, Integer pageSize);
}
