package com.edu.backend.agent;

import com.edu.backend.service.GlmApiService;
import com.edu.backend.service.RAGKnowledgeBaseService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class QAAgent implements BaseAgent {

    private final GlmApiService glmApiService;
    private final RAGKnowledgeBaseService ragService;
    private final AgentRegistry agentRegistry;
    private AgentState state = AgentState.IDLE;

    public QAAgent(GlmApiService glmApiService, RAGKnowledgeBaseService ragService,
                   AgentRegistry agentRegistry) {
        this.glmApiService = glmApiService;
        this.ragService = ragService;
        this.agentRegistry = agentRegistry;
    }

    @PostConstruct
    public void init() {
        agentRegistry.register(AgentType.QA, this);
    }

    @Override
    public AgentResult processTask(Task task) {
        updateState(AgentState.PROCESSING);
        long startTime = System.currentTimeMillis();
        try {
            String question = task.getContent() != null ? task.getContent() : "";

            Map<String, Object> ragResult = ragService.enhancedQA(question, null);
            String answer = (String) ragResult.get("answer");

            if (answer == null || answer.contains("抱歉") || answer.contains("没有找到")) {
                answer = glmApiService.chat("请详细回答以下学习问题：" + question);
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("question", question);
            result.put("answer", answer);
            result.put("references", ragResult.get("citations"));
            result.put("hint", generateHint(question));

            AgentResult agentResult = AgentResult.success(result);
            agentResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            return agentResult;
        } catch (Exception e) {
            try {
                String fallback = glmApiService.chat(task.getContent());
                return AgentResult.success(Map.of("question", task.getContent(), "answer", fallback));
            } catch (Exception ex) {
                return AgentResult.failure("答疑Agent执行失败: " + e.getMessage());
            }
        } finally {
            updateState(AgentState.IDLE);
        }
    }

    private String generateHint(String question) {
        return "提示：遇到类似问题可以查看课程资料中的相关章节，或向教学Agent请求详细讲解。";
    }

    @Override
    public void updateState(AgentState state) { this.state = state; }

    @Override
    public AgentType getAgentType() { return AgentType.QA; }

    @Override
    public AgentState getCurrentState() { return state; }

    @Override
    public List<String> getCapabilities() {
        return Arrays.asList("知识问答", "困惑解答", "资料检索", "问题引导");
    }
}
