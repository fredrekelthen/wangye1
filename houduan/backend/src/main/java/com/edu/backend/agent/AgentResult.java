package com.edu.backend.agent;

import java.util.Map;

public class AgentResult {
    private boolean success;
    private Object data;
    private Map<String, Object> metadata;
    private String errorMessage;
    private long executionTimeMs;

    public AgentResult() {}

    public static AgentResult success(Object data) {
        AgentResult r = new AgentResult();
        r.success = true;
        r.data = data;
        return r;
    }

    public static AgentResult failure(String error) {
        AgentResult r = new AgentResult();
        r.success = false;
        r.errorMessage = error;
        return r;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }
}
