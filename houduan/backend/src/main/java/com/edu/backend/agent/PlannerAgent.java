package com.edu.backend.agent;

import com.edu.backend.llm.LlmCallOptions;
import com.edu.backend.model.MaterialChunk;
import com.edu.backend.service.LlmService;
import com.edu.backend.service.RAGKnowledgeBaseService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

/**
 * 规划Agent：一次性产出学习规划。
 *
 * <p><b>改造说明</b>：原先分 5 次调用模型（目标分析 / 学习路径 / 建议Agent /
 * 步骤估算 / 前置知识各一次），每次都重复传输同一段学习目标并承担完整推理开销。
 * 现改为<b>单次调用返回结构化 JSON</b>：时延从 5 次往返降为 1 次，
 * 且各字段基于同一份思考产出，彼此更一致。</p>
 */
@Service
public class PlannerAgent implements BaseAgent {

    /** 规划需一次产出 5 个字段，额度取 5000 以容纳推理开销。 */
    private static final LlmCallOptions PLANNER_OPTIONS = LlmCallOptions.defaults()
            .withTemperature(0.5)
            .withMaxTokens(5000)
            .withTimeout(Duration.ofSeconds(180));

    /** 要求模型输出的 JSON 结构。 */
    private static final String PLAN_SCHEMA = """
            {
              "analysis": "学习目标分析，明确核心知识点和预期成果",
              "learningPath": ["循序渐进的学习路径，共5-8步，每步一句话"],
              "suggestedAgents": ["建议协作的Agent类型，从 规划/教学/评估/答疑/推荐/监控 中选择"],
              "estimatedSteps": 5,
              "prerequisites": ["学习该内容所需的前置知识，共3-5项"]
            }""";

    private final LlmService llmService;
    private final RAGKnowledgeBaseService ragService;
    private final AgentRegistry agentRegistry;
    private AgentState currentState = AgentState.IDLE;

    public PlannerAgent(LlmService llmService, RAGKnowledgeBaseService ragService,
                        AgentRegistry agentRegistry) {
        this.llmService = llmService;
        this.ragService = ragService;
        this.agentRegistry = agentRegistry;
    }

    @PostConstruct
    public void init() {
        agentRegistry.register(AgentType.PLANNER, this);
    }

    @Override
    public AgentType getAgentType() { return AgentType.PLANNER; }

    @Override
    public List<String> getCapabilities() {
        return List.of("规划Agent：分析学习目标、制定学习路径、建议所需 Agents、估算步骤、识别前置知识。");
    }

    @Override
    public AgentState getCurrentState() { return currentState; }

    @Override
    public void updateState(AgentState state) { this.currentState = state; }

    @Override
    public AgentResult processTask(Task task) {
        updateState(AgentState.PROCESSING);
        long startTime = System.currentTimeMillis();
        try {
            String content = task.getContent() != null ? task.getContent() : "";

            // 接入 RAG：为规划提供课程资料依据（失败自动降级，不影响主流程）
            List<MaterialChunk> chunks = retrieveChunks(content);
            Map<String, Object> plan = generatePlan(content, buildRagContext(chunks));

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("taskId", task.getId());
            result.put("analysis", plan.get("analysis"));
            result.put("learningPath", plan.get("learningPath"));
            result.put("suggestedAgents", plan.get("suggestedAgents"));
            result.put("estimatedSteps", plan.get("estimatedSteps"));
            result.put("prerequisites", plan.get("prerequisites"));
            result.put("referenceMaterials", buildReferences(chunks));

            AgentResult agentResult = AgentResult.success(result);
            agentResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            return agentResult;
        } catch (Exception e) {
            return AgentResult.failure("规划Agent执行失败: " + e.getMessage());
        } finally {
            updateState(AgentState.IDLE);
        }
    }

    /**
     * 单次调用产出完整规划。
     *
     * <p>字段缺失时回退为默认值，保证返回结构完整（下游 TutorAgent 会读取
     * {@code prerequisites}，不能出现缺键）。</p>
     */
    private Map<String, Object> generatePlan(String content, String ragContext) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请为以下学习目标制定学习规划。\n");
        prompt.append("严格只输出一个 JSON 对象，不要输出任何解释文字或代码块标记，结构如下：\n");
        prompt.append(PLAN_SCHEMA).append("\n");
        if (ragContext != null && !ragContext.isEmpty()) {
            prompt.append("\n可参考以下课程资料：\n").append(ragContext).append("\n");
        }
        prompt.append("\n学习目标：\n").append(content);

        Map<String, Object> parsed = AgentJsonSupport.parseObject(
                llmService.chatStrict(prompt.toString(), PLANNER_OPTIONS));

        Map<String, Object> plan = new LinkedHashMap<>();
        plan.put("analysis", AgentJsonSupport.asString(parsed, "analysis", ""));
        plan.put("learningPath", AgentJsonSupport.asStringList(parsed.get("learningPath")));
        plan.put("suggestedAgents", AgentJsonSupport.asStringList(parsed.get("suggestedAgents")));
        plan.put("estimatedSteps", AgentJsonSupport.asInt(parsed, "estimatedSteps", 5));
        plan.put("prerequisites", AgentJsonSupport.asStringList(parsed.get("prerequisites")));
        return plan;
    }

    private List<MaterialChunk> retrieveChunks(String topic) {
        try {
            List<MaterialChunk> chunks = ragService.retrieveRelevantChunks(topic, 3);
            return chunks == null ? new ArrayList<>() : chunks;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String buildRagContext(List<MaterialChunk> chunks) {
        if (chunks.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (MaterialChunk c : chunks) {
            sb.append("【").append(c.getMaterialTitle() != null ? c.getMaterialTitle() : "资料")
              .append("】").append(c.getContent()).append("\n");
        }
        return sb.toString();
    }

    private List<Map<String, Object>> buildReferences(List<MaterialChunk> chunks) {
        List<Map<String, Object>> refs = new ArrayList<>();
        for (MaterialChunk c : chunks) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("title", c.getMaterialTitle());
            r.put("content", c.getContent());
            refs.add(r);
        }
        return refs;
    }

    public String getAgentName() { return "规划Agent"; }
    public String getAgentDescription() { return "负责分析目标与制定学习路径"; }
}
