package com.edu.backend.agent;

import com.edu.backend.llm.LlmCallOptions;
import com.edu.backend.service.LlmService;
import com.edu.backend.service.RAGKnowledgeBaseService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

/**
 * 推荐Agent：一次性产出资源推荐与学习技巧。
 *
 * <p><b>改造说明</b>：原先分 2 次调用模型（资源推荐 / 学习技巧各一次），
 * 现合并为<b>单次调用返回结构化 JSON</b>。{@code learningSequence} 为固定教学阶段，
 * 不含个性化信息，保持本地生成，不消耗模型调用。</p>
 */
@Service
public class RecommenderAgent implements BaseAgent {

    private static final LlmCallOptions RECOMMENDER_OPTIONS = LlmCallOptions.defaults()
            .withTemperature(0.7)
            .withMaxTokens(4000)
            .withTimeout(Duration.ofSeconds(150));

    /** 要求模型输出的 JSON 结构。 */
    private static final String RECOMMEND_SCHEMA = """
            {
              "recommendedResources": [
                {"type": "资源类型（教材/在线课程/实践项目等）", "description": "一句简介"}
              ],
              "studyTips": ["实用学习技巧，共3条，每条不超过30字"]
            }""";

    private final LlmService llmService;
    private final RAGKnowledgeBaseService ragService;
    private final AgentRegistry agentRegistry;
    private AgentState state = AgentState.IDLE;

    public RecommenderAgent(LlmService llmService, RAGKnowledgeBaseService ragService,
                            AgentRegistry agentRegistry) {
        this.llmService = llmService;
        this.ragService = ragService;
        this.agentRegistry = agentRegistry;
    }

    @PostConstruct
    public void init() {
        agentRegistry.register(AgentType.RECOMMENDER, this);
    }

    @Override
    public AgentResult processTask(Task task) {
        updateState(AgentState.PROCESSING);
        long startTime = System.currentTimeMillis();
        try {
            String content = task.getContent() != null ? task.getContent() : "";

            Map<String, Object> generated = generateRecommendations(content);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("recommendedResources", generated.get("recommendedResources"));
            result.put("learningSequence", defaultSequence());
            result.put("studyTips", generated.get("studyTips"));

            AgentResult agentResult = AgentResult.success(result);
            agentResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            return agentResult;
        } catch (Exception e) {
            return AgentResult.failure("推荐Agent执行失败: " + e.getMessage());
        } finally {
            updateState(AgentState.IDLE);
        }
    }

    /** 单次调用产出资源推荐与学习技巧；字段缺失时用兜底内容填充。 */
    private Map<String, Object> generateRecommendations(String content) {
        String topic = content.length() > 100 ? content.substring(0, 100) : content;
        String ragContext = buildRagContext(topic);

        StringBuilder prompt = new StringBuilder();
        prompt.append("请为学习【").append(topic).append("】给出推荐。\n");
        prompt.append("严格只输出一个 JSON 对象，不要输出任何解释文字或代码块标记，结构如下：\n");
        prompt.append(RECOMMEND_SCHEMA).append("\n");
        prompt.append("recommendedResources 给出 3 种不同类型的学习资源。\n");
        if (!ragContext.isEmpty()) {
            prompt.append("\n可参考以下课程资料：\n").append(ragContext);
        }

        Map<String, Object> parsed = AgentJsonSupport.parseObject(
                llmService.chatStrict(prompt.toString(), RECOMMENDER_OPTIONS));

        List<Map<String, String>> resources = normalizeResources(parsed.get("recommendedResources"));
        List<String> tips = AgentJsonSupport.asStringList(parsed.get("studyTips"));
        if (tips.isEmpty()) {
            tips.add("建议制定学习计划，循序渐进掌握知识");
        }

        Map<String, Object> generated = new LinkedHashMap<>();
        generated.put("recommendedResources", resources);
        generated.put("studyTips", tips);
        return generated;
    }

    /** 规整资源列表：兼容 [{type,description}] 与纯字符串数组两种形态。 */
    private List<Map<String, String>> normalizeResources(Object value) {
        List<Map<String, String>> resources = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object one : list) {
                if (one instanceof Map<?, ?> map) {
                    Map<String, String> item = new LinkedHashMap<>();
                    item.put("type", stringOr(map.get("type"), "学习资源"));
                    item.put("description", stringOr(map.get("description"), ""));
                    resources.add(item);
                } else if (one != null) {
                    Map<String, String> item = new LinkedHashMap<>();
                    item.put("type", "学习资源");
                    item.put("description", String.valueOf(one).trim());
                    resources.add(item);
                }
                if (resources.size() >= 3) {
                    break;
                }
            }
        }
        if (resources.isEmpty()) {
            resources.add(Map.of("type", "教材", "description", "选择一本经典的入门教材系统学习"));
            resources.add(Map.of("type", "在线课程", "description", "推荐MOOC平台上的相关课程"));
            resources.add(Map.of("type", "实践项目", "description", "通过动手项目巩固所学知识"));
        }
        return resources;
    }

    private String stringOr(Object value, String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? defaultValue : text;
    }

    private String buildRagContext(String topic) {
        try {
            var chunks = ragService.retrieveRelevantChunks(topic, 3);
            if (chunks == null || chunks.isEmpty()) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            for (var c : chunks) {
                sb.append("【").append(c.getMaterialTitle() != null ? c.getMaterialTitle() : "资料")
                  .append("】").append(c.getContent()).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /** 固定教学阶段：与个性化内容无关，本地生成即可。 */
    private List<Map<String, String>> defaultSequence() {
        List<Map<String, String>> sequence = new ArrayList<>();
        sequence.add(Map.of("phase", "基础入门", "focus", "掌握核心概念和术语"));
        sequence.add(Map.of("phase", "深入学习", "focus", "理解原理和机制"));
        sequence.add(Map.of("phase", "实践应用", "focus", "通过项目/练习巩固"));
        sequence.add(Map.of("phase", "拓展延伸", "focus", "阅读进阶资料和论文"));
        return sequence;
    }

    @Override
    public void updateState(AgentState state) { this.state = state; }

    @Override
    public AgentType getAgentType() { return AgentType.RECOMMENDER; }

    @Override
    public AgentState getCurrentState() { return state; }

    @Override
    public List<String> getCapabilities() {
        return Arrays.asList("学习资源推荐", "学习路径优化", "学习策略建议");
    }
}
