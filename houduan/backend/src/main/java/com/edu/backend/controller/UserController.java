package com.edu.backend.controller;

import com.edu.backend.model.dto.UserDeleteRequest;
import com.edu.backend.model.dto.UserLoginRequest;
import com.edu.backend.model.dto.UserQueryRequest;
import com.edu.backend.model.dto.UserRegisterRequest;
import com.edu.backend.model.entity.User;
import com.edu.backend.repository.UserRepository;
import com.edu.backend.security.JwtUtil;
import com.edu.backend.security.SecurityUtils;
import com.edu.backend.service.UserService;
import com.edu.backend.utils.BaseResponse;
import com.edu.backend.utils.ResultUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private UserRepository userRepository;

    @Resource
    private JwtUtil jwtUtil;

    /**
     * 校验当前会话用户是否为管理员，非管理员抛出异常。
     */
    private void checkAdminPermission(HttpServletRequest request) {
        // JWT 已带 ADMIN 角色，直接放行
        if (SecurityUtils.isJwtAdmin()) {
            return;
        }
        // 兼容旧 session 鉴权
        String userId = SecurityUtils.getCurrentUserId(request);
        if (userId == null || userId.isEmpty()) {
            throw new RuntimeException("用户未登录，请先登录");
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        String role = user.getRole();
        String userRole = user.getUserRole();
        boolean isAdmin = "ADMIN".equals(role) || "admin".equals(userRole) || "ADMIN".equals(userRole);
        if (!isAdmin) {
            throw new RuntimeException("无权限访问，仅限管理员操作");
        }
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        try {
            Long result = userService.userRegister(userRegisterRequest);
            return ResultUtils.success(result, "注册成功");
        } catch (Exception e) {
            log.error("用户注册失败：{}", e.getMessage());
            return ResultUtils.error(40000, e.getMessage());
        }
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public BaseResponse<Map<String, Object>> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        try {
            User user = userService.userLogin(userLoginRequest);
            // 无状态鉴权：仅签发 JWT
            String token = jwtUtil.generateToken(user.getId(), user.getRole());
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("user", user);

            log.info("用户 {} 登录成功，角色：{}", user.getUserAccount(), user.getRole());

            return ResultUtils.success(data, "登录成功");
        } catch (Exception e) {
            log.error("用户登录失败：{}", e.getMessage());
            return ResultUtils.error(40000, e.getMessage());
        }
    }

    /**
     * 获取当前登录用户
     */
    @GetMapping("/current")
    public BaseResponse<User> getCurrentUser(HttpServletRequest request) {
        try {
            // 优先 JWT，其次兼容旧 session
            String userId = SecurityUtils.getCurrentUserId(request);
            if (userId == null || userId.isEmpty()) {
                return ResultUtils.error(40101, "用户未登录");
            }
            User user = userService.getCurrentUser(userId);
            return ResultUtils.success(user);
        } catch (Exception e) {
            log.error("获取当前用户失败：{}", e.getMessage());
            return ResultUtils.error(40100, e.getMessage());
        }
    }

    /**
     * 用户注销
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        try {
            String userId = SecurityUtils.getCurrentUserId(request);
            userService.userLogout(userId);
            return ResultUtils.success(true, "注销成功");
        } catch (Exception e) {
            log.error("用户注销失败：{}", e.getMessage());
            return ResultUtils.error(40000, e.getMessage());
        }
    }

    /**
     * 获取用户列表（管理员权限）
     */
    @GetMapping("/list")
    public BaseResponse<Object> getUserList(UserQueryRequest userQueryRequest, HttpServletRequest request) {
        try {
            checkAdminPermission(request);
            Object userList = userService.getUserList(userQueryRequest);
            return ResultUtils.success(userList);
        } catch (Exception e) {
            log.error("获取用户列表失败：{}", e.getMessage());
            return ResultUtils.error(40000, e.getMessage());
        }
    }

    /**
     * 删除用户（管理员权限）- POST方式
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteUserByPost(@RequestBody UserDeleteRequest userDeleteRequest, HttpServletRequest request) {
        try {
            checkAdminPermission(request);
            Boolean result = userService.deleteUser(userDeleteRequest.getId());
            return ResultUtils.success(result, "删除成功");
        } catch (Exception e) {
            log.error("删除用户失败：{}", e.getMessage());
            return ResultUtils.error(40000, e.getMessage());
        }
    }

    /**
     * 删除用户（管理员权限）- DELETE方式
     */
    @DeleteMapping("/delete/{userId}")
    public BaseResponse<Boolean> deleteUserByPath(@PathVariable String userId, HttpServletRequest request) {
        try {
            checkAdminPermission(request);
            Boolean result = userService.deleteUser(userId);
            return ResultUtils.success(result, "删除成功");
        } catch (Exception e) {
            log.error("删除用户失败：{}", e.getMessage());
            return ResultUtils.error(40000, e.getMessage());
        }
    }

    /**
     * 修改用户密码（管理员功能）
     */
    @PutMapping("/password")
    public BaseResponse<Boolean> updatePassword(@RequestBody Map<String, String> body, HttpServletRequest request) {
        try {
            checkAdminPermission(request);
            String userId = body.get("id");
            String newPassword = body.get("userPassword");
            if (userId == null || userId.isEmpty()) {
                return ResultUtils.error(40000, "用户ID不能为空");
            }
            if (newPassword == null || newPassword.length() < 6) {
                return ResultUtils.error(40000, "密码长度不能少于6位");
            }
            Boolean result = userService.updatePassword(userId, newPassword);
            return ResultUtils.success(result, "修改成功");
        } catch (Exception e) {
            log.error("修改密码失败：{}", e.getMessage());
            return ResultUtils.error(40000, e.getMessage());
        }
    }

    /**
     * 用户自助修改密码（需登录，校验旧密码）
     */
    @PutMapping("/self-password")
    public BaseResponse<Boolean> changeSelfPassword(@RequestBody Map<String, String> body, HttpServletRequest request) {
        try {
            String userId = SecurityUtils.getCurrentUserId(request);
            if (userId == null || userId.isEmpty()) {
                return ResultUtils.error(40101, "用户未登录");
            }
            String oldPassword = body.get("oldPassword");
            String newPassword = body.get("newPassword");
            Boolean result = userService.changeSelfPassword(userId, oldPassword, newPassword);
            return ResultUtils.success(result, "密码修改成功");
        } catch (Exception e) {
            log.error("自助修改密码失败：{}", e.getMessage());
            return ResultUtils.error(40000, e.getMessage());
        }
    }
}
