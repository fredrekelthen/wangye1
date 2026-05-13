package com.edu.backend.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

/**
 * 课程结构实体（内嵌文档设计）
 * MongoDB Collection: course_structures
 * 包含完整的三级层级：章(Chapter) -> 节(Section) -> 小节(Subsection)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "course_structures")
public class CourseStructure {

    /**
     * 主键
     */
    @Id
    private String id;

    /**
     * 课程名称（如：电磁场课程）
     */
    private String courseName;

    /**
     * 课程描述
     */
    private String description;

    /**
     * 课程封面图URL
     */
    private String coverUrl;

    /**
     * 章节列表（内嵌文档）
     */
    private List<CourseChapter> chapters;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 课程状态：PUBLISHED(已发布), DRAFT(草稿)
     */
    private String status;

    /**
     * 课程标签
     */
    private List<String> tags;

    /**
     * 章节实体（内嵌文档）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseChapter {
        /**
         * 章节ID（如：chapter-1）
         */
        private String id;

        /**
         * 章节标题（如：第1章 矢量分析）
         */
        private String title;

        /**
         * 章节描述
         */
        private String description;

        /**
         * 章节颜色（用于前端展示）
         */
        private String color;

        /**
         * 排序序号
         */
        private Integer sortOrder;

        /**
         * 节列表（内嵌文档）
         */
        private List<CourseSection> sections;
    }

    /**
     * 节实体（内嵌文档）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseSection {
        /**
         * 节ID（如：1-1）
         */
        private String id;

        /**
         * 节标题（如：1.1 矢量代数）
         */
        private String title;

        /**
         * 节描述
         */
        private String description;

        /**
         * 排序序号
         */
        private Integer sortOrder;

        /**
         * 小节列表（内嵌文档）
         */
        private List<CourseSubsection> subsections;
    }

    /**
     * 小节实体（内嵌文档）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseSubsection {
        /**
         * 小节ID（如：1-1-1）
         */
        private String id;

        /**
         * 小节标题（如：1.1.1 标量和矢量）
         */
        private String title;

        /**
         * 小节描述
         */
        private String description;

        /**
         * 排序序号
         */
        private Integer sortOrder;

        /**
         * 学习状态：UNREAD(未读), IN_PROGRESS(学习中), COMPLETED(已完成)
         */
        private String status;

        /**
         * 学习进度（0-100）
         */
        private Integer progress;
    }
}
