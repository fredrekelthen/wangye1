package com.edu.backend.service.impl;

import com.edu.backend.model.dto.UserLoginRequest;
import com.edu.backend.model.dto.UserQueryRequest;
import com.edu.backend.model.dto.UserRegisterRequest;
import com.edu.backend.model.entity.User;
import com.edu.backend.repository.UserRepository;
import com.edu.backend.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Resource
    private UserRepository userRepository;

    @Resource
    private MongoTemplate mongoTemplate;

    /**
     * 密码加密盐值
     */
    private static final String SALT = "edu_platform_salt";

    /**
     * 用户注册
     */
    @Override
    public Long userRegister(UserRegisterRequest userRegisterRequest) {
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String planetCode = userRegisterRequest.getPlanetCode();

        // 1. 校验
        if (userAccount == null || userAccount.isEmpty()) {
            throw new RuntimeException("账号不能为空");
        }
        if (userPassword == null || userPassword.isEmpty()) {
            throw new RuntimeException("密码不能为空");
        }
        if (userPassword.length() < 6) {
            throw new RuntimeException("密码长度不能少于6位");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new RuntimeException("两次密码输入不一致");
        }

        // 2. 检查是否为管理员账号（禁止注册）
        if ("admin".equalsIgnoreCase(userAccount)) {
            throw new RuntimeException("管理员账号不允许注册");
        }

        // 3. 检查账号是否已存在
        if (userRepository.existsByUserAccount(userAccount)) {
            throw new RuntimeException("账号已存在");
        }

        // 4. 检查编号是否已存在（如果编号唯一）
        User existingPlanetCode = userRepository.findByPlanetCode(planetCode).orElse(null);
        if (existingPlanetCode != null) {
            throw new RuntimeException("编号已存在");
        }

        // 5. 加密密码
        String encryptPassword = encryptPassword(userPassword);

        // 6. 创建用户
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setPlanetCode(planetCode);
        user.setRole("USER"); // 默认为普通用户
        user.setUserRole("user"); // 前端兼容字段
        user.setUserName(userAccount); // 默认用户名为账号
        user.setUserStatus(0); // 0-正常
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());

        // 7. 保存用户
        userRepository.save(user);
        log.info("用户注册成功：{}", userAccount);
        return 1L;
    }

    /**
     * 用户登录
     */
    @Override
    public User userLogin(UserLoginRequest userLoginRequest) {
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();

        // 1. 校验
        if (userAccount == null || userAccount.isEmpty()) {
            throw new RuntimeException("账号不能为空");
        }
        if (userPassword == null || userPassword.isEmpty()) {
            throw new RuntimeException("密码不能为空");
        }

        // 2. 加密密码
        String encryptPassword = encryptPassword(userPassword);

        // 3. 查询用户
        User user = userRepository.findByUserAccountAndUserPassword(userAccount, encryptPassword)
                .orElse(null);
        if (user == null) {
            log.info("用户登录失败：账号或密码错误");
            throw new RuntimeException("账号或密码错误");
        }

        // 4. 检查用户状态
        if (user.getUserStatus() == 1) {
            throw new RuntimeException("账号已被封禁");
        }

        // 5. 确保前端兼容字段同步
        if (user.getUserRole() == null) {
            user.setUserRole(user.getRole());
        }
        // 6. 返回用户信息（不返回密码）
        user.setUserPassword(null);
        log.info("用户登录成功：{}", userAccount);
        return user;
    }

    /**
     * 获取当前登录用户
     */
    @Override
    public User getCurrentUser(String userId) {
        if (userId == null || userId.isEmpty()) {
            throw new RuntimeException("用户未登录");
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 不返回密码
        user.setUserPassword(null);
        return user;
    }

    /**
     * 用户注销
     */
    @Override
    public Boolean userLogout(String userId) {
        // 这里可以根据需要实现Session清理等逻辑
        log.info("用户注销成功：{}", userId);
        return true;
    }

    /**
     * 获取用户列表（分页）
     */
    @Override
    public Object getUserList(UserQueryRequest userQueryRequest) {
        Integer pageNum = userQueryRequest.getPageNum();
        Integer pageSize = userQueryRequest.getPageSize();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String role = userQueryRequest.getRole();
        Integer userStatus = userQueryRequest.getUserStatus();

        // 构建查询条件
        Query query = new Query();

        if (userAccount != null && !userAccount.isEmpty()) {
            query.addCriteria(Criteria.where("userAccount").regex(userAccount, "i"));
        }
        if (userName != null && !userName.isEmpty()) {
            query.addCriteria(Criteria.where("userName").regex(userName, "i"));
        }
        if (role != null && !role.isEmpty()) {
            query.addCriteria(Criteria.where("role").is(role));
        }
        if (userStatus != null) {
            query.addCriteria(Criteria.where("userStatus").is(userStatus));
        }

        // 分页
        Pageable pageable = PageRequest.of(pageNum - 1, pageSize, Sort.by(Sort.Direction.DESC, "createTime"));
        long total = mongoTemplate.count(query, User.class);
        query.with(pageable);

        // 执行查询
        java.util.List<User> users = mongoTemplate.find(query, User.class);

        // 管理员查看用户列表时保留密码信息（用于管理操作）
        // users.forEach(user -> user.setUserPassword(null));

        // 计算总页数
        int totalPages = (int) Math.ceil((double) total / pageSize);

        // 返回分页结果（使用Map简化）
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("content", users);
        result.put("totalPages", totalPages);
        result.put("totalElements", total);
        result.put("number", pageNum - 1);
        result.put("size", pageSize);
        result.put("hasContent", !users.isEmpty());
        result.put("hasNext", pageNum < totalPages);
        result.put("hasPrevious", pageNum > 1);
        result.put("isFirst", pageNum == 1);
        result.put("isLast", pageNum >= totalPages);

        return result;
    }

    /**
     * 删除用户
     */
    @Override
    public Boolean deleteUser(String userId) {
        if (userId == null || userId.isEmpty()) {
            throw new RuntimeException("用户ID不能为空");
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        userRepository.deleteById(userId);
        log.info("用户删除成功：{}", userId);
        return true;
    }

    /**
     * 修改用户密码（管理员功能）
     */
    @Override
    public Boolean updatePassword(String userId, String newPassword) {
        if (userId == null || userId.isEmpty()) {
            throw new RuntimeException("用户ID不能为空");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("密码长度不能少于6位");
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 加密新密码并保存
        String encryptPassword = encryptPassword(newPassword);
        user.setUserPassword(encryptPassword);
        user.setUpdateTime(new Date());
        userRepository.save(user);

        log.info("管理员修改用户 {} 的密码成功", user.getUserAccount());
        return true;
    }

    /**
     * 密码加密（简单的MD5加密，实际建议使用BCrypt）
     */
    private String encryptPassword(String password) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest((password + SALT).getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("密码加密失败");
        }
    }
}
