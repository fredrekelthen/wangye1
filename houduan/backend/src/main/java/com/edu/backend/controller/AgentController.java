package com.edu.backend.controller;

import com.edu.backend.model.entity.User;
import com.edu.backend.repository.UserRepository;
import com.edu.backend.security.SecurityUtils;
import com.edu.backend.service.AgentService;
import com.edu.backend.service.DatabaseService;
import com.edu.backend.service.GlmApiService;
import com.edu.backend.service.RAGKnowledgeBaseService;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

@Slf4j
@RestController
@RequestMapping("/api/agent")
@CrossOrigin(origins = "*")
public class AgentController {

    @Autowired
    private AgentService agentService;

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private GlmApiService glmApiService;

    @Autowired
    private RAGKnowledgeBaseService ragService;

    @Autowired
    private UserRepository userRepository;

    /**
     * 校验当前会话用户是否为管理员。用于数据库自省、组件分析等敏感调试接口，
     * 防止未授权用户拖库或读取任意文件。
     */
    private void checkAdminPermission(HttpServletRequest request) {
        // JWT 已带 ADMIN 角色，直接放行
        if (SecurityUtils.isJwtAdmin()) {
            return;
        }
        String userId = SecurityUtils.getCurrentUserId(request);
        if (userId == null || userId.isEmpty()) {
            throw new RuntimeException("用户未登录，请先登录");
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        String role = user.getRole();
        String userRole = user.getUserRole();
        boolean isAdmin = "ADMIN".equals(role) || "admin".equals(userRole) || "ADMIN".equals(userRole);
        if (!isAdmin) {
            throw new RuntimeException("无权限访问，仅限管理员操作");
        }
    }

    @GetMapping("/project/structure")
    public ResponseEntity<Map<String, Object>> getProjectStructure() {
        try {
            Map<String, Object> structure = agentService.getProjectStructure();
            return ResponseEntity.ok(structure);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/routes")
    public ResponseEntity<Map<String, Object>> getRoutes() {
        try {
            Map<String, Object> routes = agentService.getRoutes();
            return ResponseEntity.ok(routes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/components")
    public ResponseEntity<Map<String, Object>> getComponents() {
        try {
            Map<String, Object> components = agentService.getComponents();
            return ResponseEntity.ok(components);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/component/analyze")
    public ResponseEntity<Map<String, Object>> analyzeComponent(@RequestBody Map<String, String> body, HttpServletRequest request) {
        try {
            checkAdminPermission(request);
            String path = body.get("path");
            Map<String, Object> analysis = agentService.analyzeComponent(path);
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/database/schema")
    public ResponseEntity<Map<String, Object>> getDatabaseSchema(HttpServletRequest request) {
        try {
            checkAdminPermission(request);
            Map<String, Object> schema = databaseService.getDatabaseSchema();
            return ResponseEntity.ok(schema);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/database/collections")
    public ResponseEntity<Map<String, Object>> getCollections(HttpServletRequest request) {
        try {
            checkAdminPermission(request);
            Map<String, Object> collections = databaseService.getCollections();
            return ResponseEntity.ok(collections);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/database/query")
    public ResponseEntity<Map<String, Object>> queryDatabase(@RequestBody Map<String, String> body, HttpServletRequest request) {
        try {
            checkAdminPermission(request);
            String query = body.get("query");
            Map<String, Object> result = databaseService.executeQuery(query);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/navigate")
    public ResponseEntity<Map<String, Object>> navigateToPage(@RequestBody Map<String, String> request) {
        try {
            String page = request.get("page");
            agentService.navigateToPage(page);
            return ResponseEntity.ok(Map.of("success", true, "message", "导航成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, Object> request) {
        try {
            String prompt = (String) request.get("prompt");
            Map<String, Object> context = (Map<String, Object>) request.get("context");
            
            Map<String, Object> result = new HashMap<>();
            
            String pageToNavigate = detectNavigation(prompt);
            if (pageToNavigate != null) {
                result.put("data", "好的，已为您跳转到对应的页面");
                result.put("action", "navigate");
                result.put("page", pageToNavigate);
                result.put("citations", Collections.emptyList());
            } else {
                Map<String, Object> ragResult = enhancedChatWithRAG(prompt);
                result.put("data", ragResult.get("answer"));
                result.put("citations", ragResult.getOrDefault("citations", Collections.emptyList()));
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 使用RAG知识库增强的聊天——返回结构化结果含引用
     */
    private Map<String, Object> enhancedChatWithRAG(String prompt) {
        try {
            String lowerPrompt = prompt.toLowerCase();
            
            if (containsAny(lowerPrompt, "学习", "课程", "知识点", "章节", "内容", "讲解", "解释", "什么是", "如何", "为什么", "怎么做")) {
                Map<String, Object> ragResult = ragService.enhancedQA(
                    prompt, 
                    "抱歉，我没有找到相关的学习资料来回答您的问题。"
                );
                
                String answer = (String) ragResult.get("answer");
                if (answer != null && !answer.isEmpty() && !answer.contains("抱歉")) {
                    return ragResult;
                }
            }
            
            String fallback = glmApiService.chat(prompt);
            Map<String, Object> result = new HashMap<>();
            result.put("answer", fallback);
            result.put("citations", Collections.emptyList());
            return result;
            
        } catch (Exception e) {
            log.warn("RAG增强聊天失败: " + e.getMessage());
            try {
                String fallback = glmApiService.chat(prompt);
                Map<String, Object> result = new HashMap<>();
                result.put("answer", fallback);
                result.put("citations", Collections.emptyList());
                return result;
            } catch (Exception ex) {
                Map<String, Object> result = new HashMap<>();
                result.put("answer", "抱歉，服务暂时不可用，请稍后再试。");
                result.put("citations", Collections.emptyList());
                return result;
            }
        }
    }

    /**
     * 获取知识库统计信息
     */
    @GetMapping("/knowledge/stats")
    public ResponseEntity<Map<String, Object>> getKnowledgeStats() {
        try {
            Map<String, Object> stats = ragService.getKnowledgeBaseStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 重建BM25倒排索引（管理后台触发）
     */
    @PostMapping("/knowledge/rebuild-bm25")
    public ResponseEntity<Map<String, Object>> rebuildBM25() {
        try {
            Map<String, Object> stats = ragService.rebuildBM25Index();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 重试处理 PENDING 状态的chunk
     */
    @PostMapping("/knowledge/retry-pending")
    public ResponseEntity<Map<String, Object>> retryPendingChunks(@RequestBody Map<String, String> request) {
        try {
            String materialId = request.get("materialId");
            int success = ragService.retryPendingChunks(materialId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            result.put("message", "成功处理 " + success + " 个chunk");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 基于知识库生成学习内容
     */
    @GetMapping("/learning/generate/{chapter}")
    public ResponseEntity<Map<String, Object>> generateLearningContent(@PathVariable Integer chapter) {
        try {
            Map<String, Object> result = new HashMap<>();

            // 复用按章全量召回的生成逻辑：
            // 原先此处用 top-5 语义检索，整章内容大部分拿不到，模型会误判"资料里没有这一章"。
            Map<String, Object> generated = ragService.generateChapterStudyContent(chapter);

            if (generated.get("materialCount") instanceof Integer count && count == 0) {
                result.put("success", false);
                result.put("message", "该章节暂无相关资料");
                return ResponseEntity.ok(result);
            }

            String content = generated.get("answer") == null ? "" : generated.get("answer").toString();
            if (content.isBlank()) {
                result.put("success", false);
                result.put("message", "生成失败，请稍后重试");
                return ResponseEntity.ok(result);
            }

            result.put("success", true);
            result.put("chapter", chapter);
            result.put("content", content);
            result.put("sources", generated.get("chunkCount"));

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    private String detectNavigation(String prompt) {
        if (prompt == null) return null;
        
        String lowerPrompt = prompt.toLowerCase();
        
        if (containsAny(lowerPrompt, "首页", "主页", "home", "回到首页", "去首页", "打开首页", "去主页")) {
            return "/";
        }
        if (containsAny(lowerPrompt, "课程", "course", "学习课程", "课程页面", "课程学习", "学习页面", "打开课程", "去课程", "学习", "课程中心")) {
            return "/course";
        }
        if (containsAny(lowerPrompt, "用户中心", "user-center", "个人中心", "我的", "个人资料", "打开个人中心", "去用户中心")) {
            return "/user-center";
        }
        if (containsAny(lowerPrompt, "资料管理", "material-manage", "管理资料", "资料", "打开资料管理", "去资料管理")) {
            return "/admin/material-manage";
        }
        if (containsAny(lowerPrompt, "用户管理", "user-manage", "管理用户", "学生管理", "打开用户管理", "去用户管理")) {
            return "/admin/user-manage";
        }
        
        return null;
    }
    
    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
