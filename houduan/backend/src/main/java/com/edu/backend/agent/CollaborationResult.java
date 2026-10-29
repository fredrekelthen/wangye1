package com.edu.backend.agent;

import java.util.List;
import java.util.Map;

public class CollaborationResult {
    private String taskId;
    private String originalTask;
    private List<Map<String, Object>> agentOutputs;
    private AgentResult planResult;
    private AgentResult teachingResult;
    private AgentResult assessmentResult;
    private AgentResult qaResult;
    private AgentResult recommendationResult;
    private AgentResult monitoringResult;
    private AgentResult codeResult;
    private String summary;
    private Map<String, Object> timestamps;

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public String getOriginalTask() { return originalTask; }
    public void setOriginalTask(String originalTask) { this.originalTask = originalTask; }
    public List<Map<String, Object>> getAgentOutputs() { return agentOutputs; }
    public void setAgentOutputs(List<Map<String, Object>> agentOutputs) { this.agentOutputs = agentOutputs; }
    public AgentResult getPlanResult() { return planResult; }
    public void setPlanResult(AgentResult planResult) { this.planResult = planResult; }
    public AgentResult getTeachingResult() { return teachingResult; }
    public void setTeachingResult(AgentResult teachingResult) { this.teachingResult = teachingResult; }
    public AgentResult getAssessmentResult() { return assessmentResult; }
    public void setAssessmentResult(AgentResult assessmentResult) { this.assessmentResult = assessmentResult; }
    public AgentResult getQaResult() { return qaResult; }
    public void setQaResult(AgentResult qaResult) { this.qaResult = qaResult; }
    public AgentResult getRecommendationResult() { return recommendationResult; }
    public void setRecommendationResult(AgentResult recommendationResult) { this.recommendationResult = recommendationResult; }
    public AgentResult getMonitoringResult() { return monitoringResult; }
    public void setMonitoringResult(AgentResult monitoringResult) { this.monitoringResult = monitoringResult; }
    public AgentResult getCodeResult() { return codeResult; }
    public void setCodeResult(AgentResult codeResult) { this.codeResult = codeResult; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public Map<String, Object> getTimestamps() { return timestamps; }
    public void setTimestamps(Map<String, Object> timestamps) { this.timestamps = timestamps; }
}
