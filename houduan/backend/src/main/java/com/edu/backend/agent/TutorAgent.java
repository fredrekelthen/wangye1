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
 * 教学Agent：一次性生成完整教学材料。
 *
 * <p><b>改造说明</b>：原先对模型的调用被拆成 7 次（定义 / 知识点 / 讲解 / 示例 /
 * 习题 / 误解 / 检查点各一次），每次都要承担完整的推理开销，总耗时和费用都很高，
 * 且任一环节失败都会造成该字段缺失。现改为<b>单次调用返回结构化 JSON</b>，
 * 上下文一次给全，字段间也更连贯。</p>
 *
 * <p>同时把原先重复执行的两次 RAG 检索（参考资料 + 讲解依据）合并为一次。</p>
 */
@Service
public class TutorAgent implements BaseAgent {

    /**
     * 教学材料单次产出的调用选项。
     * 需同时容纳 7 个字段（讲解约 400 字）与推理模型的思考开销，故额度取 6000。
     */
    private static final LlmCallOptions TUTOR_OPTIONS = LlmCallOptions.defaults()
            .withTemperature(0.6)
            .withMaxTokens(6000)
            .withTimeout(Duration.ofSeconds(180));

    /** 要求模型输出的 JSON 结构（字段与前端消费的键一一对应）。 */
    private static final String TEACHING_SCHEMA = """
            {
              "conceptDefinition": "概念定义，2-3句话",
              "keyPoints": ["关键知识点，共5-7条"],
              "detailedExplanation": "通俗易懂的详细讲解，含定义、原理、应用场景、注意事项，约200-400字",
              "examples": ["具体应用示例或案例，共3个"],
              "exercises": ["练习题及答案要点，共3道"],
              "commonMisconceptions": ["学习该主题时常见的误解，共3-5条"],
              "knowledgeCheckpoints": ["用于自我检测的知识检查点，共4条"]
            }""";

    private final LlmService llmService;
    private final RAGKnowledgeBaseService ragService;
    private final AgentRegistry agentRegistry;
    private AgentState currentState = AgentState.IDLE;

    public TutorAgent(LlmService llmService, RAGKnowledgeBaseService ragService,
                      AgentRegistry agentRegistry) {
        this.llmService = llmService;
        this.ragService = ragService;
        this.agentRegistry = agentRegistry;
    }

    @PostConstruct
    public void init() {
        agentRegistry.register(AgentType.TUTOR, this);
    }

    @Override
    public AgentType getAgentType() { return AgentType.TUTOR; }

    @Override
    public List<String> getCapabilities() {
        return List.of("教学Agent：生成详细的教学材料和讲解，包括概念定义、重点、详细解释、示例、练习、常见误解和知识检查点。");
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
            String domain = task.getDomain() != null ? task.getDomain() : "";
            String topic = extractTopic(content);

            // 接入共享记忆：读取上游（规划Agent）产出，使教学衔接学习路径/前置知识
            String prerequisitesHint = extractPrerequisitesFromContext(task.getContext());

            // 接入 RAG：一次检索同时供"参考资料"与"讲解依据"使用（原先检索了两次）
            List<MaterialChunk> chunks = retrieveChunks(topic);
            String ragContext = buildRagContext(chunks);

            Map<String, Object> generated = generateTeachingMaterial(topic, domain, ragContext);

            Map<String, Object> teachingMaterial = new LinkedHashMap<>();
            teachingMaterial.put("topic", topic);
            teachingMaterial.put("conceptDefinition", generated.get("conceptDefinition"));
            teachingMaterial.put("keyPoints", generated.get("keyPoints"));
            teachingMaterial.put("detailedExplanation", generated.get("detailedExplanation"));
            teachingMaterial.put("examples", generated.get("examples"));
            teachingMaterial.put("exercises", generated.get("exercises"));
            teachingMaterial.put("commonMisconceptions", generated.get("commonMisconceptions"));
            teachingMaterial.put("knowledgeCheckpoints", generated.get("knowledgeCheckpoints"));
            if (!prerequisitesHint.isEmpty()) {
                teachingMaterial.put("relatedPrerequisites", prerequisitesHint);
            }
            teachingMaterial.put("referenceMaterials", buildReferences(chunks));

            AgentResult result = AgentResult.success(teachingMaterial);
            result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            return result;
        } catch (Exception e) {
            return AgentResult.failure("教学Agent执行失败: " + e.getMessage());
        } finally {
            updateState(AgentState.IDLE);
        }
    }

    /**
     * 单次调用产出全部教学字段。
     *
     * <p>解析不到某字段时该字段回退为默认值（空串或空列表），
     * 保证返回结构始终完整，前端无需处理缺键。</p>
     */
    private Map<String, Object> generateTeachingMaterial(String topic, String domain, String ragContext) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请为【").append(topic).append("】（领域：").append(domain).append("）生成教学材料。\n");
        prompt.append("严格只输出一个 JSON 对象，不要输出任何解释文字或代码块标记，结构如下：\n");
        prompt.append(TEACHING_SCHEMA).append("\n");
        if (ragContext != null && !ragContext.isEmpty()) {
            prompt.append("\n可参考以下课程资料，内容须与资料保持一致：\n").append(ragContext);
        }

        Map<String, Object> parsed = AgentJsonSupport.parseObject(
                llmService.chatStrict(prompt.toString(), TUTOR_OPTIONS));

        Map<String, Object> material = new LinkedHashMap<>();
        material.put("conceptDefinition",
                AgentJsonSupport.asString(parsed, "conceptDefinition", "暂无定义"));
        material.put("keyPoints",
                AgentJsonSupport.asStringList(parsed.get("keyPoints")));
        material.put("detailedExplanation",
                AgentJsonSupport.asString(parsed, "detailedExplanation", "暂无讲解"));
        material.put("examples",
                AgentJsonSupport.asStringList(parsed.get("examples")));
        material.put("exercises",
                AgentJsonSupport.asStringList(parsed.get("exercises")));
        material.put("commonMisconceptions",
                AgentJsonSupport.asStringList(parsed.get("commonMisconceptions")));
        material.put("knowledgeCheckpoints",
                AgentJsonSupport.asStringList(parsed.get("knowledgeCheckpoints")));
        return material;
    }

    /** RAG 检索（失败降级为空列表，不影响主流程）。 */
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

    @SuppressWarnings("unchecked")
    private String extractPrerequisitesFromContext(Map<String, Object> context) {
        if (context == null || context.isEmpty()) return "";
        Object planner = context.get("PLANNER");
        if (!(planner instanceof Map)) return "";
        Object pre = ((Map<String, Object>) planner).get("prerequisites");
        if (pre instanceof List) {
            StringBuilder sb = new StringBuilder();
            for (Object o : (List<Object>) pre) sb.append(o).append("、");
            if (sb.length() > 0) sb.setLength(sb.length() - 1);
            return sb.toString();
        }
        return "";
    }

    private String extractTopic(String content) {
        List<String> keywords = Arrays.asList("概念", "理解", "学习", "讲解", "什么", "如何", "原理", "定义");
        String best = "";
        for (String kw : keywords) {
            int idx = content.indexOf(kw);
            if (idx >= 0) {
                int start = Math.max(0, idx - 15);
                int end = Math.min(content.length(), idx + 15);
                String candidate = content.substring(start, end).replaceAll("[，。？！,.?!]", " ").trim();
                if (candidate.length() > best.length()) best = candidate;
            }
        }
        return best.isEmpty()
                ? content.substring(0, Math.min(content.length(), 20))
                : best;
    }

    public String getAgentName() { return "教学Agent"; }
    public String getAgentDescription() { return "负责生成详细的教学材料和讲解"; }
}
