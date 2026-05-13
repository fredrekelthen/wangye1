package com.edu.backend.service.impl;

import com.edu.backend.model.entity.UserGrade;
import com.edu.backend.model.entity.UserLearning;
import com.edu.backend.model.entity.UserProgress;
import com.edu.backend.repository.UserGradeRepository;
import com.edu.backend.repository.UserLearningRepository;
import com.edu.backend.repository.UserProgressRepository;
import com.edu.backend.service.UserCenterService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 个人中心服务实现类
 */
@Slf4j
@Service
public class UserCenterServiceImpl implements UserCenterService {

    @Resource
    private UserProgressRepository userProgressRepository;

    @Resource
    private UserLearningRepository userLearningRepository;

    @Resource
    private UserGradeRepository userGradeRepository;

    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public Map<String, Object> getUserCenterStats(String userId) {
        Map<String, Object> stats = new HashMap<>();

        // 账号名称
        stats.put("userAccount", getUserAccount(userId));

        // 学习进度
        long totalCount = userProgressRepository.countAllByUserId(userId);
        long completedCount = userProgressRepository.countCompletedByUserId(userId);
        double progressRate = totalCount > 0 ? (double) completedCount / totalCount * 100 : 0;

        Map<String, Object> progress = new HashMap<>();
        progress.put("total", totalCount);
        progress.put("completed", completedCount);
        progress.put("rate", Math.round(progressRate * 10.0) / 10.0); // 保留一位小数
        stats.put("learningProgress", progress);

        // 学习次数统计
        long totalStudyCount = userLearningRepository.countByUserId(userId);
        stats.put("studyCount", totalStudyCount);

        // 近期查看资料（最近5条）
        List<UserLearning> recentLearnings = userLearningRepository.findRecentByUserId(userId, PageRequest.of(0, 5));
        List<Map<String, Object>> recentMaterials = recentLearnings.stream().map(learning -> {
            Map<String, Object> material = new HashMap<>();
            material.put("id", learning.getMaterialId());
            material.put("name", learning.getMaterialName());
            material.put("type", learning.getMaterialType());
            material.put("chapterTitle", learning.getChapterTitle());
            material.put("subsectionTitle", learning.getSubsectionTitle());
            material.put("studyTime", learning.getStudyTime());
            return material;
        }).collect(Collectors.toList());
        stats.put("recentMaterials", recentMaterials);

        // 个人成绩
        List<UserGrade> grades = userGradeRepository.findByUserIdOrderByExamTimeDesc(userId);
        double averageScore = 0;
        if (!grades.isEmpty()) {
            averageScore = grades.stream().mapToInt(UserGrade::getScore).average().orElse(0);
        }
        Map<String, Object> gradeStats = new HashMap<>();
        gradeStats.put("average", Math.round(averageScore));
        gradeStats.put("count", grades.size());
        gradeStats.put("recent", grades.stream().limit(5).map(this::gradeToMap).collect(Collectors.toList()));
        stats.put("gradeStats", gradeStats);

        return stats;
    }

    @Override
    public Map<String, Object> getLearningProgressByDay(String userId, int days) {
        Map<String, Object> result = new HashMap<>();

        // 获取最近N天的学习记录
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        Date startTime = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endTime = Date.from(endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

        List<UserLearning> learnings = userLearningRepository.findByUserIdAndStudyTimeBetween(userId, startTime, endTime);

        // 按日期分组统计
        Map<String, Long> dailyCounts = learnings.stream()
                .collect(Collectors.groupingBy(
                        learning -> {
                            LocalDate date = learning.getStudyTime().toInstant()
                                    .atZone(ZoneId.systemDefault()).toLocalDate();
                            return date.toString();
                        },
                        Collectors.counting()
                ));

        // 生成日期列表（确保连续）
        List<String> dates = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = endDate.minusDays(i);
            dates.add(date.toString());
            counts.add(dailyCounts.getOrDefault(date.toString(), 0L));
        }

        result.put("dates", dates);
        result.put("counts", counts);

        return result;
    }

    @Override
    public Object getRecentMaterials(String userId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        List<UserLearning> learnings = userLearningRepository.findRecentByUserId(userId, pageable);

        List<Map<String, Object>> materials = learnings.stream().map(learning -> {
            Map<String, Object> material = new HashMap<>();
            material.put("id", learning.getMaterialId());
            material.put("name", learning.getMaterialName());
            material.put("type", learning.getMaterialType());
            material.put("chapterTitle", learning.getChapterTitle());
            material.put("subsectionTitle", learning.getSubsectionTitle());
            material.put("studyTime", learning.getStudyTime());
            material.put("studyDuration", learning.getStudyDuration());
            return material;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("content", materials);
        result.put("total", materials.size());

        return result;
    }

    @Override
    public Object getUserGrades(String userId, Integer pageNum, Integer pageSize) {
        pageNum = pageNum != null && pageNum > 0 ? pageNum : 1;
        pageSize = pageSize != null && pageSize > 0 ? pageSize : 10;

        Pageable pageable = PageRequest.of(pageNum - 1, pageSize);
        var page = userGradeRepository.findByUserIdOrderByExamTimeDesc(userId, pageable);

        List<Map<String, Object>> grades = page.getContent().stream()
                .map(this::gradeToMap)
                .collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("content", grades);
        result.put("totalPages", page.getTotalPages());
        result.put("totalElements", page.getTotalElements());
        result.put("number", page.getNumber());
        result.put("size", page.getSize());
        result.put("hasContent", page.hasContent());
        result.put("hasNext", page.hasNext());
        result.put("hasPrevious", page.hasPrevious());
        result.put("isFirst", page.isFirst());
        result.put("isLast", page.isLast());

        return result;
    }

    private String getUserAccount(String userId) {
        Query query = new Query(Criteria.where("id").is(userId));
        query.fields().include("userAccount");
        com.edu.backend.model.entity.User user = mongoTemplate.findOne(query, com.edu.backend.model.entity.User.class, "users");
        return user != null ? user.getUserAccount() : null;
    }

    private Map<String, Object> gradeToMap(UserGrade grade) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", grade.getId());
        map.put("examType", grade.getExamType());
        map.put("examName", grade.getExamName());
        map.put("score", grade.getScore());
        map.put("fullScore", grade.getFullScore());
        map.put("passScore", grade.getPassScore());
        map.put("isPassed", grade.getScore() >= grade.getPassScore());
        map.put("examTime", grade.getExamTime());
        return map;
    }
}
