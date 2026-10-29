package com.edu.backend.conversation;

import com.edu.backend.conversation.model.ChatMessage;
import com.edu.backend.conversation.service.ConversationService;
import com.edu.backend.utils.BaseResponse;
import com.edu.backend.utils.ResultUtils;
import com.edu.backend.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 对话会话 REST 控制器。
 * 所有接口均从当前登录态读取 userId，保证用户间数据隔离。
 */
@RestController
@RequestMapping("/api/conversation")
public class ConversationController {

    private final ConversationService service;

    public ConversationController(ConversationService service) {
        this.service = service;
    }

    // ---------- 工具：取当前用户ID ----------
    private String currentUserId(HttpServletRequest req) {
        String userId = SecurityUtils.getCurrentUserId(req);
        if (userId == null || userId.isEmpty()) {
            throw new RuntimeException("用户未登录");
        }
        return userId;
    }

    // ---------- 新建空会话 ----------
    @PostMapping("/new")
    public BaseResponse<Map<String, Object>> newConversation(HttpServletRequest req) {
        try {
            String userId = currentUserId(req);
            String id = service.createConversation(userId);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("conversationId", id);
            return ResultUtils.success(data, "会话创建成功");
        } catch (Exception e) {
            return ResultUtils.error(40000, e.getMessage());
        }
    }

    // ---------- 会话内发消息 ----------
    @PostMapping("/{id}/message")
    public BaseResponse<Map<String, Object>> sendMessage(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            HttpServletRequest req) {
        try {
            String userId = currentUserId(req);
            String content = body.get("content");
            if (content == null || content.trim().isEmpty()) {
                return ResultUtils.error(40000, "消息内容不能为空");
            }
            Map<String, Object> result = service.sendMessage(userId, id, content);
            return ResultUtils.success(result, "发送成功");
        } catch (Exception e) {
            return ResultUtils.error(40000, e.getMessage());
        }
    }

    // ---------- 会话列表 ----------
    @GetMapping("/list")
    public BaseResponse<List<Map<String, Object>>> listConversations(HttpServletRequest req) {
        try {
            String userId = currentUserId(req);
            List<Map<String, Object>> list = service.listConversations(userId);
            return ResultUtils.success(list);
        } catch (Exception e) {
            return ResultUtils.error(40000, e.getMessage());
        }
    }

    // ---------- 回看会话消息 ----------
    @GetMapping("/{id}/messages")
    public BaseResponse<List<ChatMessage>> getMessages(
            @PathVariable String id, HttpServletRequest req) {
        try {
            String userId = currentUserId(req);
            List<ChatMessage> messages = service.getMessages(userId, id);
            return ResultUtils.success(messages);
        } catch (Exception e) {
            return ResultUtils.error(40000, e.getMessage());
        }
    }

    // ---------- 重命名 ----------
    @PutMapping("/{id}/title")
    public BaseResponse<Boolean> renameConversation(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            HttpServletRequest req) {
        try {
            String userId = currentUserId(req);
            String title = body.get("title");
            service.renameConversation(userId, id, title);
            return ResultUtils.success(true, "重命名成功");
        } catch (Exception e) {
            return ResultUtils.error(40000, e.getMessage());
        }
    }

    // ---------- 删除 ----------
    @DeleteMapping("/{id}")
    public BaseResponse<Boolean> deleteConversation(
            @PathVariable String id, HttpServletRequest req) {
        try {
            String userId = currentUserId(req);
            service.deleteConversation(userId, id);
            return ResultUtils.success(true, "删除成功");
        } catch (Exception e) {
            return ResultUtils.error(40000, e.getMessage());
        }
    }
}
