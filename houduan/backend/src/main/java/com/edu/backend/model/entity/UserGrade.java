package com.edu.backend.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * 用户成绩实体
 * MongoDB Collection: user_grades
 * 用于记录用户考试成绩
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_grades")
@CompoundIndex(def = "{'userId': 1, 'examType': 1}")
public class UserGrade {

    /**
     * 主键
     */
    @Id
    private String id;

    /**
     * 用户ID
     */
    @Indexed
    private String userId;

    /**
     * 考试类型：QUIZ(测验), EXAM(考试), FINAL(期末考试)
     */
    @Indexed
    private String examType;

    /**
     * 考试名称（如：第1章测验、期中考试）
     */
    private String examName;

    /**
     * 分数
     */
    private Integer score;

    /**
     * 满分
     */
    private Integer fullScore;

    /**
     * 及格线
     */
    private Integer passScore;

    /**
     * 考试时间
     */
    private Date examTime;

    /**
     * 创建时间
     */
    private Date createTime;
}
