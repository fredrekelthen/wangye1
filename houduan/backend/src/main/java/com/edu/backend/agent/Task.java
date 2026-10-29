package com.edu.backend.agent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Task {
    private String id;
    private String userId;
    private String domain;
    private String content;
    private Map<String, Object> params;
    private String deadline;
    private TaskPriority priority;
    private TaskStatus status;
    private long createdAt;
    // 跨智能体上下文：协作管理器在执行下游 Agent 前，注入上游 Agent 的产出
    private Map<String, Object> context;

    public Task() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = System.currentTimeMillis();
        this.status = TaskStatus.CREATED;
        this.priority = TaskPriority.NORMAL;
        this.context = new HashMap<>();
    }

    public enum TaskPriority { LOW, NORMAL, HIGH, URGENT }
    public enum TaskStatus { CREATED, DECOMPOSED, IN_PROGRESS, COMPLETED, FAILED }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Map<String, Object> getParams() { return params; }
    public void setParams(Map<String, Object> params) { this.params = params; }
    public String getDeadline() { return deadline; }
    public void setDeadline(String deadline) { this.deadline = deadline; }
    public TaskPriority getPriority() { return priority; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public Map<String, Object> getContext() { return context; }
    public void setContext(Map<String, Object> context) { this.context = context; }
}
