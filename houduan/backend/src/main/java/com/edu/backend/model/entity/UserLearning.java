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
 * 用户学习记录实体
 * MongoDB Collection: user_learning
 * 用于记录用户每次学习行为
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user_learning")
@CompoundIndex(def = "{'userId': 1, 'materialId': 1}")
public class UserLearning {

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
     * 资料ID（查看的学习资料）
     */
    @Indexed
    private String materialId;

    /**
     * 资料名称
     */
    private String materialName;

    /**
     * 资料类型：PDF, DOCX, 视频, PPT
     */
    private String materialType;

    /**
     * 小节ID
     */
    private String subsectionId;

    /**
     * 小节标题
     */
    private String subsectionTitle;

    /**
     * 章节标题
     */
    private String chapterTitle;

    /**
     * 学习时长（分钟）
     */
    private Integer studyDuration;

    /**
     * 学习时间
     */
    private Date studyTime;

    /**
     * 创建时间
     */
    private Date createTime;
}
