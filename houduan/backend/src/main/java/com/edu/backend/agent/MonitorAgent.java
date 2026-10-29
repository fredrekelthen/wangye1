package com.edu.backend.agent;

import com.edu.backend.service.GlmApiService;
import com.edu.backend.service.RAGKnowledgeBaseService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MonitorAgent implements BaseAgent {

    private final GlmApiService glmApiService;
    private final RAGKnowledgeBaseService ragService;
    private final AgentRegistry agentRegistry;
    private AgentState state = AgentState.IDLE;

    public MonitorAgent(GlmApiService glmApiService, RAGKnowledgeBaseService ragService,
                        AgentRegistry agentRegistry) {
        this.glmApiService = glmApiService;
        this.ragService = ragService;
        this.agentRegistry = agentRegistry;
    }

    @PostConstruct
    public void init() {
        agentRegistry.register(AgentType.MONITOR, this);
    }

    @Override
    public AgentResult processTask(Task task) {
        updateState(AgentState.PROCESSING);
        long startTime = System.currentTimeMillis();
        try {
            Map<String, Object> monitoringReport = generateMonitoringReport(task);
            AgentResult result = AgentResult.success(monitoringReport);
            result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            return result;
        } catch (Exception e) {
            return AgentResult.failure("监督Agent执行失败: " + e.getMessage());
        } finally {
            updateState(AgentState.IDLE);
        }
    }

    private Map<String, Object> generateMonitoringReport(Task task) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("taskId", task.getId());
        report.put("monitoringStatus", "active");
        report.put("warnings", generateWarnings(task));
        report.put("suggestions", generateSuggestions(task));
        report.put("anomalyScore", 0.0);
        return report;
    }

    private List<String> generateWarnings(Task task) {
        List<String> warnings = new ArrayList<>();
        Task.TaskPriority priority = task.getPriority();
        if (priority == Task.TaskPriority.URGENT || priority == Task.TaskPriority.HIGH) {
            warnings.add("该任务优先级较高，建议及时完成");
        }
        if (task.getDeadline() != null && !task.getDeadline().isEmpty()) {
            warnings.add("任务包含截止日期：" + task.getDeadline() + "，请合理安排时间");
        }
        return warnings;
    }

    private List<Map<String, String>> generateSuggestions(Task task) {
        List<Map<String, String>> suggestions = new ArrayList<>();
        suggestions.add(Map.of("type", "时间管理", "content", "建议将学习时间分成25分钟的专注块"));
        suggestions.add(Map.of("type", "效率提升", "content", "学习新概念后立即做相关练习，加深记忆"));
        suggestions.add(Map.of("type", "知识巩固", "content", "定期回顾已学内容，间隔重复效果最佳"));
        return suggestions;
    }

    @Override
    public void updateState(AgentState state) { this.state = state; }

    @Override
    public AgentType getAgentType() { return AgentType.MONITOR; }

    @Override
    public AgentState getCurrentState() { return state; }

    @Override
    public List<String> getCapabilities() {
        return Arrays.asList("学习行为监控", "异常检测", "干预建议", "进度跟踪");
    }
}
