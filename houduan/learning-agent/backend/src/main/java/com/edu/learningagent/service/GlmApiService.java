package com.edu.learningagent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class GlmApiService {

    @Value("${glm.api.key:YOUR_GLM_API_KEY_HERE}")
    private String apiKey;

    @Value("${glm.api.url:https://open.bigmodel.cn/api/paas/v4/chat/completions}")
    private String apiUrl;

    @Value("${glm.model:glm-4}")
    private String model;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GlmApiService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public String chat(String prompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);

            List<Map<String, String>> messages = new ArrayList<>();
            
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是一个专业的项目学习助手，专注于帮助用户理解项目结构、代码逻辑和业务功能。你应该：\n" +
                    "1. 清晰解释项目的架构和技术栈\n" +
                    "2. 分析和说明组件的功能和用法\n" +
                    "3. 解释数据库设计和数据关系\n" +
                    "4. 提供代码优化的建议\n" +
                    "5. 帮助理解业务流程\n\n" +
                    "回答要简洁、专业、易于理解。");
            messages.add(systemMessage);

            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.add(userMessage);

            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 2000);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                entity,
                String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode responseJson = objectMapper.readTree(response.getBody());
                
                if (responseJson.has("choices") && 
                    responseJson.get("choices").size() > 0) {
                    
                    JsonNode choice = responseJson.get("choices").get(0);
                    if (choice.has("message") && choice.get("message").has("content")) {
                        return choice.get("message").get("content").asText();
                    }
                }
                
                return "无法解析API响应";
            } else {
                return "API请求失败: " + response.getStatusCode();
            }

        } catch (Exception e) {
            return "与GLM API通信时发生错误: " + e.getMessage();
        }
    }

    public Map<String, Object> chatWithContext(String prompt, Map<String, Object> context) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            StringBuilder fullPrompt = new StringBuilder();
            fullPrompt.append(prompt);
            fullPrompt.append("\n\n项目上下文信息:\n");
            
            if (context != null) {
                if (context.containsKey("type")) {
                    fullPrompt.append("- 问题类型: ").append(context.get("type")).append("\n");
                }
                if (context.containsKey("projectPath")) {
                    fullPrompt.append("- 项目路径: ").append(context.get("projectPath")).append("\n");
                }
                if (context.containsKey("routes")) {
                    fullPrompt.append("- 可用路由: ").append(context.get("routes")).append("\n");
                }
                if (context.containsKey("components")) {
                    fullPrompt.append("- 组件信息: ").append(context.get("components")).append("\n");
                }
                if (context.containsKey("database")) {
                    fullPrompt.append("- 数据库信息: ").append(context.get("database")).append("\n");
                }
            }
            
            String response = chat(fullPrompt.toString());
            result.put("response", response);
            result.put("success", true);
            
        } catch (Exception e) {
            result.put("error", e.getMessage());
            result.put("success", false);
        }
        
        return result;
    }

    public boolean validateApiKey() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", Arrays.asList(
                Map.of("role", "user", "content", "Hello")
            ));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                entity,
                String.class
            );

            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            return false;
        }
    }
}
