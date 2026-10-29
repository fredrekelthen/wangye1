package com.edu.backend.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SubTask {
    private String id;
    private String parentTaskId;
    private AgentType assignedAgent;
    private String description;
    private Object inputSpec;
    private Object outputSpec;
    private Task.TaskPriority priority;
    private SubTaskStatus status;
    private List<String> dependencies;

    public SubTask() {
        this.id = UUID.randomUUID().toString();
        this.status = SubTaskStatus.PENDING;
        this.dependencies = new ArrayList<>();
    }

    public enum SubTaskStatus { PENDING, IN_PROGRESS, COMPLETED, FAILED, WAITING }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getParentTaskId() { return parentTaskId; }
    public void setParentTaskId(String parentTaskId) { this.parentTaskId = parentTaskId; }
    public AgentType getAssignedAgent() { return assignedAgent; }
    public void setAssignedAgent(AgentType assignedAgent) { this.assignedAgent = assignedAgent; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Object getInputSpec() { return inputSpec; }
    public void setInputSpec(Object inputSpec) { this.inputSpec = inputSpec; }
    public Object getOutputSpec() { return outputSpec; }
    public void setOutputSpec(Object outputSpec) { this.outputSpec = outputSpec; }
    public Task.TaskPriority getPriority() { return priority; }
    public void setPriority(Task.TaskPriority priority) { this.priority = priority; }
    public SubTaskStatus getStatus() { return status; }
    public void setStatus(SubTaskStatus status) { this.status = status; }
    public List<String> getDependencies() { return dependencies; }
    public void setDependencies(List<String> dependencies) { this.dependencies = dependencies; }
}
