package com.edu.learningagent.controller;

import com.edu.learningagent.service.AgentService;
import com.edu.learningagent.service.DatabaseService;
import com.edu.learningagent.service.GlmApiService;
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

    @PostMapping("/component/open")
    public ResponseEntity<Map<String, Object>> openComponent(@RequestBody Map<String, String> request) {
        try {
            String path = request.get("path");
            agentService.openComponentInEditor(path);
            return ResponseEntity.ok(Map.of("success", true, "message", "组件已打开"));
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
            
            String projectContext = agentService.getProjectContext();
            String fullPrompt = prompt + "\n\n项目上下文:\n" + projectContext;
            
            String response = glmApiService.chat(fullPrompt);
            return ResponseEntity.ok(Map.of("data", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
