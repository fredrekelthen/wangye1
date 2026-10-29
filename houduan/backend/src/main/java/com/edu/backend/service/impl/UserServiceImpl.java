package com.edu.backend.service.impl;

import com.edu.backend.model.dto.UserLoginRequest;
import com.edu.backend.model.dto.UserQueryRequest;
import com.edu.backend.model.dto.UserRegisterRequest;
import com.edu.backend.model.entity.User;
import com.edu.backend.repository.UserRepository;
import com.edu.backend.service.UserService;
import com.edu.backend.exception.BusinessException;
import com.edu.backend.model.enums.ErrorCode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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

    /** BCrypt 密码编码器（线程安全，用于新密码哈希与校验） */
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 旧 MD5 加密盐值（仅用于存量密码兼容比对，勿用于新密码）。
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

        // 5. BCrypt 加密密码
        String encryptPassword = passwordEncoder.encode(userPassword);

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

        // 2. 按账号查询用户（密码在代码中校验，兼容存量 MD5 与 BCrypt）
        User user = userRepository.findByUserAccount(userAccount).orElse(null);
        if (user == null) {
            log.info("用户登录失败：账号不存在");
            throw new BusinessException(ErrorCode.USER_PASSWORD_ERROR, "账号或密码错误");
        }

        // 3. 密码校验（兼容存量 MD5，通过后透明升级为 BCrypt）
        if (!passwordMatches(userPassword, user.getUserPassword())) {
            log.info("用户登录失败：密码错误");
            throw new BusinessException(ErrorCode.USER_PASSWORD_ERROR, "账号或密码错误");
        }
        if (user.getUserPassword() == null || !user.getUserPassword().startsWith("$2")) {
            user.setUserPassword(passwordEncoder.encode(userPassword));
            user.setUpdateTime(new Date());
            userRepository.save(user);
            log.info("用户 {} 的密码已从旧算法透明升级为 BCrypt", userAccount);
        }

        // 4. 检查用户状态
        if (Integer.valueOf(1).equals(user.getUserStatus())) {
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

        // 密码哈希不会外泄：User.userPassword 已标注 @JsonProperty(WRITE_ONLY)，序列化时自动剔除。

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

        // BCrypt 加密新密码并保存
        String encryptPassword = passwordEncoder.encode(newPassword);
        user.setUserPassword(encryptPassword);
        user.setUpdateTime(new Date());
        userRepository.save(user);

        log.info("管理员修改用户 {} 的密码成功", user.getUserAccount());
        return true;
    }

    /**
     * 用户自助修改密码：校验旧密码后更新为新密码（BCrypt）。
     */
    @Override
    public Boolean changeSelfPassword(String userId, String oldPassword, String newPassword) {
        if (userId == null || userId.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        }
        if (oldPassword == null || oldPassword.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请输入原密码");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新密码长度不能少于6位");
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_EXIST, "用户不存在");
        }

        // 校验旧密码（兼容存量 MD5）
        if (!passwordMatches(oldPassword, user.getUserPassword())) {
            throw new BusinessException(ErrorCode.USER_PASSWORD_ERROR, "原密码错误");
        }

        user.setUserPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(new Date());
        userRepository.save(user);

        log.info("用户 {} 自助修改密码成功", user.getUserAccount());
        return true;
    }

    /**
     * 校验密码是否匹配：存量密码为 MD5(明文+盐)，新密码为 BCrypt（以 {@code $2} 开头），二者均兼容。
     */
    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (storedPassword == null) {
            return false;
        }
        if (storedPassword.startsWith("$2")) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        return storedPassword.equals(legacyMd5(rawPassword));
    }

    /**
     * 旧 MD5 哈希（仅用于历史密码兼容比对，勿用于新密码）。
     */
    private String legacyMd5(String password) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest((password + SALT).getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("密码加密失败", e);
        }
    }
}
