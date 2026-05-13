package com.edu.backend.config;

import com.edu.backend.model.entity.CourseStructure;
import com.edu.backend.model.entity.User;
import com.edu.backend.repository.CourseRepository;
import com.edu.backend.repository.UserRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 数据初始化类
 * 在应用启动时自动初始化课程数据和管理员账号
 */
@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

        @Resource
        private CourseRepository courseRepository;

        @Resource
        private UserRepository userRepository;

        /**
         * 密码加密盐值
         */
        private static final String SALT = "edu_platform_salt";

        @Override
        public void run(String... args) {
                // 初始化管理员账号
                initAdminUser();

                // 检查是否已存在课程数据
                if (courseRepository.count() > 0) {
                log.info("课程数据已存在，跳过初始化");
                return;
                }

        log.info("开始初始化课程数据...");

        // 创建课程结构
        CourseStructure course = new CourseStructure();
        course.setCourseName("电磁场课程");
        course.setDescription("电磁场课程，涵盖矢量分析、静电场、恒定电场、恒定磁场、时变电磁场等内容");
        course.setCoverUrl("");
        course.setStatus("PUBLISHED");
        course.setTags(List.of("电磁场", "物理", "工科"));
        course.setCreateTime(new Date());
        course.setUpdateTime(new Date());

        // 创建章节列表
        List<CourseStructure.CourseChapter> chapters = new ArrayList<>();

        // 第1章 矢量分析
        CourseStructure.CourseChapter chapter1 = createChapter(
                "chapter-1",
                "第1章 矢量分析",
                "矢量代数、坐标系、梯度、散度、旋度等基础理论",
                "#00B894",
                1,
                createChapter1Sections()
        );
        chapters.add(chapter1);

        // 第2章 静电场与恒定电场
        CourseStructure.CourseChapter chapter2 = createChapter(
                "chapter-2",
                "第2章 静电场与恒定电场",
                "库仑定律、电场强度、电位函数、导体与介质",
                "#00B894",
                2,
                createChapter2Sections()
        );
        chapters.add(chapter2);

        course.setChapters(chapters);

        // 保存课程
        courseRepository.save(course);
        log.info("课程数据初始化完成");
        }

        /**
         * 创建章节
         */
        private CourseStructure.CourseChapter createChapter(
                String id,
                String title,
                String description,
                String color,
                int sortOrder,
                List<CourseStructure.CourseSection> sections
        ) {
        CourseStructure.CourseChapter chapter = new CourseStructure.CourseChapter();
        chapter.setId(id);
        chapter.setTitle(title);
        chapter.setDescription(description);
        chapter.setColor(color);
        chapter.setSortOrder(sortOrder);
        chapter.setSections(sections);
        return chapter;
        }

        /**
         * 创建第1章的节
         */
        private List<CourseStructure.CourseSection> createChapter1Sections() {
                List<CourseStructure.CourseSection> sections = new ArrayList<>();

        // 1.1 矢量代数
        sections.add(createSection(
                "1-1",
                "1.1 矢量代数",
                "标量和矢量、矢量的加减法与乘法",
                1,
                createSubsections1_1()
        ));

        // 1.2 三种常用的坐标系
        sections.add(createSection(
                "1-2",
                "1.2 三种常用的坐标系",
                "正交坐标系、直角坐标系、圆柱坐标系、球坐标系",
                2,
                createSubsections1_2()
        ));

        // 1.3 标量场的梯度
        sections.add(createSection(
                "1-3",
                "1.3 标量场的梯度",
                "标量场的等值面、方向导数、梯度",
                3,
                createSubsections1_3()
        ));

        // 1.4 矢量场的通量与散度
        sections.add(createSection(
                "1-4",
                "1.4 矢量场的通量与散度",
                "通量、散度、散度定理",
                4,
                createSubsections1_4()
        ));

        // 1.5 矢量场的环量与旋度
        sections.add(createSection(
                "1-5",
                "1.5 矢量场的环量与旋度",
                "环量、旋度、斯托克斯定理",
                5,
                createSubsections1_5()
        ));

        // 1.6 无旋场与无散场
        sections.add(createSection(
                "1-6",
                "1.6 无旋场与无散场",
                "无旋场与无散场的定义与特性",
                6,
                List.of(
                        createSubsection("1-6-1", "1.6.1 无旋场", "无旋场的定义与性质", 1),
                        createSubsection("1-6-2", "1.6.2 无散场", "无散场的定义与性质", 2)
                )
        ));

        // 1.7 拉普拉斯运算与格林定理
        sections.add(createSection(
                "1-7",
                "1.7 拉普拉斯运算与格林定理",
                "拉普拉斯运算、格林定理",
                7,
                List.of(
                        createSubsection("1-7-1", "1.7.1 拉普拉斯运算", "拉普拉斯运算的定义与性质", 1),
                        createSubsection("1-7-2", "1.7.2 格林定理", "格林定理及其应用", 2)
                )
        ));

        return sections;
        }

        /**
         * 创建第2章的节
         */
        private List<CourseStructure.CourseSection> createChapter2Sections() {
                List<CourseStructure.CourseSection> sections = new ArrayList<>();

        // 2.1 库仑定律与电场强度
        sections.add(createSection(
                "2-1",
                "2.1 库仑定律与电场强度",
                "库仑定律、电场强度及其叠加原理",
                1,
                List.of(
                        createSubsection("2-1-1", "2.1.1 库仑定律", "库仑定律的内容与应用", 1),
                        createSubsection("2-1-2", "2.1.2 电场强度及其叠加原理", "电场强度的定义与叠加原理", 2)
                )
        ));

        // 2.2 电场强度的通量和散度
        sections.add(createSection(
                "2-2",
                "2.2 电场强度的通量和散度",
                "电场强度的通量与散度",
                2,
                List.of(
                        createSubsection("2-2-1", "2.2.1 电场强度的通量", "电场强度通量的计算", 1),
                        createSubsection("2-2-2", "2.2.2 电场强度的散度", "电场强度散度的计算与性质", 2)
                )
        ));

        // 2.3 电场强度的环量及旋度
        sections.add(createSection(
                "2-3",
                "2.3 电场强度的环量及旋度",
                "电场强度的环量与旋度",
                3,
                List.of(
                        createSubsection("2-3-1", "2.3.1 电场强度的环量", "电场强度环量的计算", 1),
                        createSubsection("2-3-2", "2.3.2 电场强度的旋度", "电场强度旋度的计算与性质", 2)
                )
        ));

        return sections;
        }

        /**
         * 创建节
         */
        private CourseStructure.CourseSection createSection(
                String id,
                String title,
                String description,
                int sortOrder,
                List<CourseStructure.CourseSubsection> subsections
        ) {
                CourseStructure.CourseSection section = new CourseStructure.CourseSection();
                section.setId(id);
                section.setTitle(title);
                section.setDescription(description);
                section.setSortOrder(sortOrder);
                section.setSubsections(subsections);
                return section;
        }

        /**
         * 创建小节
         */
        private CourseStructure.CourseSubsection createSubsection(
                String id,
                String title,
                String description,
                int sortOrder
        ) {
        CourseStructure.CourseSubsection subsection = new CourseStructure.CourseSubsection();
        subsection.setId(id);
        subsection.setTitle(title);
        subsection.setDescription(description);
        subsection.setSortOrder(sortOrder);
        subsection.setStatus("UNREAD");
        subsection.setProgress(0);
                return subsection;
        }

        /**
         * 创建1.1的小节
         */
        private List<CourseStructure.CourseSubsection> createSubsections1_1() {
                return List.of(
                        createSubsection("1-1-1", "1.1.1 标量和矢量", "标量和矢量的定义与区别", 1),
                        createSubsection("1-1-2", "1.1.2 矢量的加法和减法", "矢量的加减法运算规则", 2),
                        createSubsection("1-1-3", "1.1.3 矢量的乘法", "矢量的乘法运算：标积与矢积", 3)
                );
        }

        /**
         * 创建1.2的小节
         */
        private List<CourseStructure.CourseSubsection> createSubsections1_2() {
                return List.of(
                        createSubsection("1-2-1", "1.2.1 正交坐标系", "正交坐标系的概念与性质", 1),
                        createSubsection("1-2-2", "1.2.2 直角坐标系", "直角坐标系及其性质", 2),
                        createSubsection("1-2-3", "1.2.3 圆柱坐标系", "圆柱坐标系及其性质", 3),
                        createSubsection("1-2-4", "1.2.4 球坐标系", "球坐标系及其性质", 4)
                );
        }

        /**
         * 创建1.3的小节
         */
        private List<CourseStructure.CourseSubsection> createSubsections1_3() {
                return List.of(
                        createSubsection("1-3-1", "1.3.1 标量场的等值面", "等值面的概念与性质", 1),
                        createSubsection("1-3-2", "1.3.2 方向导数", "方向导数的定义与计算", 2),
                        createSubsection("1-3-3", "1.3.3 梯度", "梯度的定义与性质", 3)
                );
        }

        /**
         * 创建1.4的小节
         */
        private List<CourseStructure.CourseSubsection> createSubsections1_4() {
                return List.of(
                        createSubsection("1-4-1", "1.4.1 通量", "通量的概念与计算", 1),
                        createSubsection("1-4-2", "1.4.2 散度", "散度的定义与性质", 2),
                        createSubsection("1-4-3", "1.4.3 散度定理", "散度定理及其应用", 3)
                );
        }

        /**
         * 创建1.5的小节
         */
        private List<CourseStructure.CourseSubsection> createSubsections1_5() {
                return List.of(
                        createSubsection("1-5-1", "1.5.1 环量", "环量的概念与计算", 1),
                        createSubsection("1-5-2", "1.5.2 旋度", "旋度的定义与性质", 2),
                        createSubsection("1-5-3", "1.5.3 斯托克斯定理", "斯托克斯定理及其应用", 3)
                );
        }

        /**
         * 初始化管理员账号
         */
        private void initAdminUser() {
                // 检查管理员账号是否已存在
                if (userRepository.existsByUserAccount("admin")) {
                        log.info("管理员账号已存在，跳过初始化");
                        return;
                }

                log.info("开始初始化管理员账号...");

                // 创建管理员账号
                User admin = new User();
                admin.setUserAccount("admin");
                admin.setUserPassword(encryptPassword("123456789"));
                admin.setPlanetCode("admin001");
                admin.setRole("ADMIN"); // 管理员角色
                admin.setUserStatus(0); // 0-正常
                admin.setUserName("系统管理员");
                admin.setCreateTime(new Date());
                admin.setUpdateTime(new Date());

                // 保存管理员
                userRepository.save(admin);
                log.info("管理员账号初始化完成：admin / 123456789");
        }

        /**
         * 密码加密
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
                        throw new RuntimeException("密码加密失败", e);
                }
        }
}
