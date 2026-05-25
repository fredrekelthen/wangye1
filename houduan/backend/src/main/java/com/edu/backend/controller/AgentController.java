package com.edu.backend.controller;

import com.edu.backend.service.AgentService;
import com.edu.backend.service.DatabaseService;
import com.edu.backend.service.GlmApiService;
import com.edu.backend.service.RAGKnowledgeBaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

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
    public ResponseEntity<Map<String, Object>> analyzeComponent(@RequestBody Map<String, String> request) {
        try {
            String path = request.get("path");
            Map<String, Object> analysis = agentService.analyzeComponent(path);
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/database/schema")
    public ResponseEntity<Map<String, Object>> getDatabaseSchema() {
        try {
            Map<String, Object> schema = databaseService.getDatabaseSchema();
            return ResponseEntity.ok(schema);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/database/collections")
    public ResponseEntity<Map<String, Object>> getCollections() {
        try {
            Map<String, Object> collections = databaseService.getCollections();
            return ResponseEntity.ok(collections);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/database/query")
    public ResponseEntity<Map<String, Object>> queryDatabase(@RequestBody Map<String, String> request) {
        try {
            String query = request.get("query");
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
            } else {
                // 检查是否是学习相关内容，使用RAG知识库
                String response = enhancedChatWithRAG(prompt);
                result.put("data", response);
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 使用RAG知识库增强的聊天
     */
    private String enhancedChatWithRAG(String prompt) {
        try {
            // 检测是否与学习内容相关
            String lowerPrompt = prompt.toLowerCase();
            
            if (containsAny(lowerPrompt, "学习", "课程", "知识点", "章节", "内容", "讲解", "解释", "什么是", "如何", "为什么", "怎么做")) {
                // 使用RAG知识库检索相关内容
                String ragResponse = ragService.enhancedQA(
                    prompt, 
                    "抱歉，我没有找到相关的学习资料来回答您的问题。"
                );
                
                if (ragResponse != null && !ragResponse.isEmpty() && !ragResponse.contains("抱歉")) {
                    return ragResponse;
                }
            }
            
            // 默认使用GLM API
            return glmApiService.chat(prompt);
            
        } catch (Exception e) {
            System.err.println("RAG增强聊天失败: " + e.getMessage());
            try {
                return glmApiService.chat(prompt);
            } catch (Exception ex) {
                return "抱歉，服务暂时不可用，请稍后再试。";
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
     * 基于知识库生成学习内容
     */
    @GetMapping("/learning/generate/{chapter}")
    public ResponseEntity<Map<String, Object>> generateLearningContent(@PathVariable Integer chapter) {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // 使用RAG检索相关资料
            String query = "第" + chapter + "章的学习内容，包括概述、知识点、重点难点";
            var relevantChunks = ragService.retrieveRelevantChunks(query, 5);
            
            if (relevantChunks.isEmpty()) {
                result.put("success", false);
                result.put("message", "该章节暂无相关资料");
                return ResponseEntity.ok(result);
            }
            
            // 构建上下文
            StringBuilder context = new StringBuilder();
            context.append("基于以下资料，生成第").append(chapter).append("章的完整学习内容：\n\n");
            
            for (var chunk : relevantChunks) {
                context.append("- ").append(chunk.getContent()).append("\n\n");
            }
            
            context.append("请生成包含以下部分的Markdown格式学习内容：\n");
            context.append("## 章节概述\n");
            context.append("## 核心知识点\n");
            context.append("## 重点难点\n");
            context.append("## 学习建议\n");
            context.append("## 相关习题\n");
            
            // 调用GLM API生成内容
            String generatedContent = glmApiService.callGlmApi(context.toString());
            
            result.put("success", true);
            result.put("chapter", chapter);
            result.put("content", generatedContent);
            result.put("sources", relevantChunks.size());
            
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
