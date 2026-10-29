package com.edu.backend.service.impl;

import com.edu.backend.model.dto.UserQueryRequest;
import com.edu.backend.model.entity.Material;
import com.edu.backend.model.entity.User;
import com.edu.backend.repository.MaterialRepository;
import com.edu.backend.repository.UserRepository;
import com.edu.backend.service.AdminService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理员服务实现类
 */
@Slf4j
@Service
public class AdminServiceImpl implements AdminService {

    @Resource
    private UserRepository userRepository;

    @Resource
    private MaterialRepository materialRepository;

    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public Object getAllUsers(UserQueryRequest userQueryRequest) {
        Integer pageNum = userQueryRequest.getPageNum() != null ? userQueryRequest.getPageNum() : 1;
        Integer pageSize = userQueryRequest.getPageSize() != null ? userQueryRequest.getPageSize() : 10;

        // 构建查询条件
        Query query = new Query();

        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String role = userQueryRequest.getRole();
        Integer userStatus = userQueryRequest.getUserStatus();

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
        List<User> users = mongoTemplate.find(query, User.class);

        // 保留密码字段（管理员可以查看）
        List<Map<String, Object>> userList = users.stream().map(user -> {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("userAccount", user.getUserAccount());
            userMap.put("userPassword", user.getUserPassword()); // 管理员可以看到密码
            userMap.put("userName", user.getUserName());
            userMap.put("planetCode", user.getPlanetCode());
            userMap.put("role", user.getRole());
            userMap.put("userStatus", user.getUserStatus());
            userMap.put("createTime", user.getCreateTime());
            return userMap;
        }).collect(Collectors.toList());

        // 计算总页数
        int totalPages = (int) Math.ceil((double) total / pageSize);

        // 返回分页结果
        Map<String, Object> result = new HashMap<>();
        result.put("content", userList);
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

    @Override
    public Object getAllMaterialsGroupedByChapter(Integer pageNum, Integer pageSize) {
        pageNum = pageNum != null && pageNum > 0 ? pageNum : 1;
        pageSize = pageSize != null && pageSize > 0 ? pageSize : 50;

        // 查询所有资料（status=0）
        Query query = new Query(Criteria.where("status").is(0));
        query.with(Sort.by(Sort.Direction.DESC, "createTime"));

        List<Material> allMaterials = mongoTemplate.find(query, Material.class);

        // 按章节分组（章节标题可能为 null，用"未分类"兜底，避免 groupingBy 空键 NPE）
        Map<String, List<Material>> groupedMaterials = allMaterials.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getChapterTitle() == null ? "未分类" : m.getChapterTitle()));

        // 转换为列表格式
        List<Map<String, Object>> chapterMaterials = new ArrayList<>();
        for (Map.Entry<String, List<Material>> entry : groupedMaterials.entrySet()) {
            Map<String, Object> chapterData = new HashMap<>();
            chapterData.put("chapterTitle", entry.getKey());
            chapterData.put("materials", entry.getValue());
            chapterMaterials.add(chapterData);
        }

        // 按章节标题排序
        chapterMaterials.sort((a, b) -> {
            String titleA = (String) a.get("chapterTitle");
            String titleB = (String) b.get("chapterTitle");
            return titleA.compareTo(titleB);
        });

        // 简单分页（按章节分组后的数量分页）
        int total = chapterMaterials.size();
        int totalPages = (int) Math.ceil((double) total / pageSize);
        // 钳制越界：请求超出末页时 fromIndex 可能大于 total，subList(from>to) 会抛异常
        int fromIndex = Math.min(Math.max((pageNum - 1) * pageSize, 0), total);
        int toIndex = Math.min(fromIndex + pageSize, total);

        List<Map<String, Object>> pagedMaterials = chapterMaterials.subList(fromIndex, toIndex);

        Map<String, Object> result = new HashMap<>();
        result.put("content", pagedMaterials);
        result.put("totalPages", totalPages);
        result.put("totalElements", total);
        result.put("number", pageNum - 1);
        result.put("size", pageSize);
        result.put("hasContent", !pagedMaterials.isEmpty());
        result.put("hasNext", pageNum < totalPages);
        result.put("hasPrevious", pageNum > 1);
        result.put("isFirst", pageNum == 1);
        result.put("isLast", pageNum >= totalPages);

        return result;
    }
}
