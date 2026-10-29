package com.edu.backend.agent;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TaskAllocator {

    private final AgentRegistry agentRegistry;

    public TaskAllocator(AgentRegistry agentRegistry) {
        this.agentRegistry = agentRegistry;
    }

    public AllocationResult allocateTask(SubTask task) {
        AgentType targetType = task.getAssignedAgent();
        List<BaseAgent> availableAgents = agentRegistry.getAgents(targetType);

        AllocationResult result = new AllocationResult();
        result.setTask(task);

        if (availableAgents.isEmpty()) {
            result.setSuccess(false);
            result.setErrorMessage("没有可用的" + targetType + "类型的Agent");
            return result;
        }

        BaseAgent selected = selectAgent(availableAgents, task);
        result.setAssignedAgent(selected);
        result.setSuccess(true);
        return result;
    }

    public List<AllocationResult> allocateTasks(List<SubTask> tasks) {
        return tasks.stream().map(this::allocateTask).collect(Collectors.toList());
    }

    private BaseAgent selectAgent(List<BaseAgent> agents, SubTask task) {
        if (agents.size() == 1) return agents.get(0);
        for (BaseAgent a : agents) {
            if (a.getCurrentState() == AgentState.IDLE) return a;
        }
        return agents.get(0);
    }

    public static class AllocationResult {
        private SubTask task;
        private BaseAgent assignedAgent;
        private boolean success;
        private String errorMessage;

        public SubTask getTask() { return task; }
        public void setTask(SubTask task) { this.task = task; }
        public BaseAgent getAssignedAgent() { return assignedAgent; }
        public void setAssignedAgent(BaseAgent assignedAgent) { this.assignedAgent = assignedAgent; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}
