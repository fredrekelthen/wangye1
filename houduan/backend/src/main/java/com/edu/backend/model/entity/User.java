package com.edu.backend.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * 用户实体
 * MongoDB Collection: users
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    /**
     * 主键（MongoDB自动生成ObjectId，这里用String接收）
     */
    @Id
    private String id;

    /**
     * 用户账号（唯一索引）
     */
    @Indexed(unique = true)
    private String userAccount;

    /**
     * 用户密码（加密存储）。
     * WRITE_ONLY：可从请求体反序列化，但绝不序列化到任何响应中，避免密码哈希外泄。
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String userPassword;

    /**
     * 编号（可选，用于标识）
     */
    private String planetCode;

    /**
     * 用户角色：USER(普通用户), ADMIN(管理员)
     */
    private String role;

    /**
     * 用户角色（前端兼容字段，与 role 同步）
     */
    private String userRole;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 用户状态：NORMAL(正常), BANNED(封禁)
     */
    private Integer userStatus;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像URL
     */
    private String userAvatar;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 电话
     */
    private String phone;
}
