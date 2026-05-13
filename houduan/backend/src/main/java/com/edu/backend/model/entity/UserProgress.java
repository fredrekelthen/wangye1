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
 * 用户学习进度实体
 * MongoDB Collection: user_progress
 * 用于记录用户对各章节的学习进度
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_progress")
@CompoundIndex(def = "{'userId': 1, 'subsectionId': 1}", unique = true)
public class UserProgress {

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
     * 小节ID（如：1-1-1）
     */
    @Indexed
    private String subsectionId;

    /**
     * 小节标题（如：1.1.1 标量和矢量）
     */
    private String subsectionTitle;

    /**
     * 章节ID（如：chapter-1）
     */
    @Indexed
    private String chapterId;

    /**
     * 章节标题（如：第1章 矢量分析）
     */
    private String chapterTitle;

    /**
     * 学习状态：UNREAD(未读), IN_PROGRESS(学习中), COMPLETED(已完成)
     */
    private String status;

    /**
     * 学习进度（0-100）
     */
    private Integer progress;

    /**
     * 最后学习时间
     */
    private Date lastStudyTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
