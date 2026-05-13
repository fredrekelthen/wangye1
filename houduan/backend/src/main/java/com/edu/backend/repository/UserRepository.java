package com.edu.backend.repository;

import com.edu.backend.model.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户数据访问接口
 */
@Repository
public interface UserRepository extends MongoRepository<User, String> {

    /**
     * 根据用户账号查询用户
     */
    Optional<User> findByUserAccount(String userAccount);

    /**
     * 检查用户账号是否存在
     */
    boolean existsByUserAccount(String userAccount);

    /**
     * 根据用户账号和密码查询用户（用于登录）
     */
    Optional<User> findByUserAccountAndUserPassword(String userAccount, String userPassword);

    /**
     * 根据角色查询用户列表
     */
    java.util.List<User> findByRole(String role);

    /**
     * 根据编号查询用户
     */
    Optional<User> findByPlanetCode(String planetCode);
}
