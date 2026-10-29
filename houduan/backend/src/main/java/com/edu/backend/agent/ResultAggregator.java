package com.edu.backend.agent;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

@Service
public class ResultAggregator {

    public CollaborationResult aggregate(List<AgentResult> results, Task originalTask) {
        CollaborationResult collaboration = new CollaborationResult();
        collaboration.setTaskId(originalTask.getId());
        collaboration.setOriginalTask(originalTask.getContent());

        Map<AgentType, AgentResult> categorized = new HashMap<>();
        for (AgentResult r : results) {
            if (r.isSuccess() && r.getMetadata() != null && r.getMetadata().containsKey("agentType")) {
                AgentType type = AgentType.valueOf((String) r.getMetadata().get("agentType"));
                categorized.put(type, r);
            }
        }

        List<Map<String, Object>> agentOutputs = new ArrayList<>();
        for (AgentResult r : results) {
            Map<String, Object> output = new LinkedHashMap<>();
            output.put("success", r.isSuccess());
            output.put("data", r.getData());
            output.put("error", r.getErrorMessage());
            output.put("executionTimeMs", r.getExecutionTimeMs());
            if (r.getMetadata() != null) {
                output.put("agentType", r.getMetadata().get("agentType"));
            }
            agentOutputs.add(output);
        }
        collaboration.setAgentOutputs(agentOutputs);

        collaboration.setPlanResult(categorized.get(AgentType.PLANNER));
        collaboration.setTeachingResult(categorized.get(AgentType.TUTOR));
        collaboration.setAssessmentResult(categorized.get(AgentType.ASSESSOR));
        collaboration.setQaResult(categorized.get(AgentType.QA));
        collaboration.setRecommendationResult(categorized.get(AgentType.RECOMMENDER));
        collaboration.setMonitoringResult(categorized.get(AgentType.MONITOR));
        collaboration.setCodeResult(categorized.get(AgentType.EM_CODE));

        collaboration.setSummary(generateSummary(collaboration));
        collaboration.setTimestamps(Map.of(
            "startTime", System.currentTimeMillis(),
            "agentCount", results.size()
        ));

        return collaboration;
    }

    private String generateSummary(CollaborationResult result) {
        StringBuilder summary = new StringBuilder();
        summary.append("【多Agent协作结果摘要】\n\n");

        int successCount = (int) result.getAgentOutputs().stream()
            .filter(o -> Boolean.TRUE.equals(o.get("success"))).count();
        summary.append("协作Agent数：").append(result.getAgentOutputs().size())
            .append("，成功：").append(successCount).append("\n\n");

        if (result.getTeachingResult() != null && result.getTeachingResult().isSuccess()) {
            summary.append("✓ 教学Agent：已生成教学材料\n");
        }
        if (result.getPlanResult() != null && result.getPlanResult().isSuccess()) {
            summary.append("✓ 规划Agent：已生成学习路径\n");
        }
        if (result.getAssessmentResult() != null && result.getAssessmentResult().isSuccess()) {
            summary.append("✓ 评估Agent：已生成评估方案\n");
        }
        if (result.getQaResult() != null && result.getQaResult().isSuccess()) {
            summary.append("✓ 答疑Agent：已完成问题解答\n");
        }
        if (result.getRecommendationResult() != null && result.getRecommendationResult().isSuccess()) {
            summary.append("✓ 推荐Agent：已生成学习建议\n");
        }

        return summary.toString();
    }
}
