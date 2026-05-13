package com.edu.backend.service;

import com.edu.backend.model.dto.UserLoginRequest;
import com.edu.backend.model.dto.UserQueryRequest;
import com.edu.backend.model.dto.UserRegisterRequest;
import com.edu.backend.model.entity.User;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 用户注册
     */
    Long userRegister(UserRegisterRequest userRegisterRequest);

    /**
     * 用户登录
     */
    User userLogin(UserLoginRequest userLoginRequest);

    /**
     * 获取当前登录用户
     */
    User getCurrentUser(String userId);

    /**
     * 用户注销
     */
    Boolean userLogout(String userId);

    /**
     * 获取用户列表（分页）
     */
    Object getUserList(UserQueryRequest userQueryRequest);

    /**
     * 删除用户
     */
    Boolean deleteUser(String userId);

    /**
     * 修改用户密码（管理员功能）
     */
    Boolean updatePassword(String userId, String newPassword);
}
