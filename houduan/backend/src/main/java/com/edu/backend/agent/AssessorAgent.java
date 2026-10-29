package com.edu.backend.agent;

import com.edu.backend.llm.LlmCallOptions;
import com.edu.backend.service.LlmService;
import com.edu.backend.service.RAGKnowledgeBaseService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

/**
 * 评估Agent：出题与评分。
 *
 * <p><b>改造说明</b>：原先"提取评估主题"与"生成测评题"是两次独立调用，
 * 现合并为<b>单次调用返回结构化 JSON</b>（主题与题目一并产出），减少一次模型往返。
 * 评分后的学习反馈发生在提交答案之后，属于独立时机，保持单独调用。</p>
 */
@Service
public class AssessorAgent implements BaseAgent {

    /** 单次调用同时产出评估主题与全部题目。 */
    private static final LlmCallOptions ASSESS_OPTIONS = LlmCallOptions.defaults()
            .withTemperature(0.6)
            .withMaxTokens(5000)
            .withTimeout(Duration.ofSeconds(180));

    /** 评分后的针对性反馈（仅在低分时触发，独立时机）。 */
    private static final LlmCallOptions FEEDBACK_OPTIONS = LlmCallOptions.defaults()
            .withTemperature(0.7)
            .withMaxTokens(1500)
            .withTimeout(Duration.ofSeconds(120));

    private final LlmService llmService;
    private final RAGKnowledgeBaseService ragService;
    private final AgentRegistry agentRegistry;
    private AgentState state = AgentState.IDLE;

    public AssessorAgent(LlmService llmService, RAGKnowledgeBaseService ragService,
                          AgentRegistry agentRegistry) {
        this.llmService = llmService;
        this.ragService = ragService;
        this.agentRegistry = agentRegistry;
    }

    @PostConstruct
    public void init() {
        agentRegistry.register(AgentType.ASSESSOR, this);
    }

    @Override
    public AgentResult processTask(Task task) {
        updateState(AgentState.PROCESSING);
        long startTime = System.currentTimeMillis();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> params = task.getParams();
            boolean hasSubmission = params != null && params.containsKey("answers");

            if (hasSubmission) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> answers = (List<Map<String, Object>>) params.get("answers");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> questions = (List<Map<String, Object>>) params.get("questions");
                Map<String, Object> evalResult = evaluateSubmission(answers, questions);
                AgentResult result = AgentResult.success(evalResult);
                result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
                return result;
            }

            Map<String, Object> assessment = generateAssessment(
                task.getContent() != null ? task.getContent() : "",
                task.getDomain() != null ? task.getDomain() : ""
            );
            AgentResult result = AgentResult.success(assessment);
            result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            return result;
        } catch (Exception e) {
            return AgentResult.failure("评估Agent执行失败: " + e.getMessage());
        } finally {
            updateState(AgentState.IDLE);
        }
    }

    /**
     * 单次调用产出评估主题与全部题目。
     *
     * <p>原先"提取主题"和"生成题目"是两次独立调用，且后者的主题依赖前者的返回，
     * 串行两次往返；现合并为一次，模型在同一份思考里产出主题与题目。</p>
     */
    private Map<String, Object> generateAssessment(String content, String domain) {
        int count = 5;

        StringBuilder prompt = new StringBuilder();
        prompt.append("请围绕以下学习内容设计一份测评。\n");
        prompt.append("严格只输出一个 JSON 对象，不要输出任何解释文字或代码块标记，结构如下：\n");
        prompt.append("{\n");
        prompt.append("  \"topic\": \"评估主题，10字以内\",\n");
        prompt.append("  \"questions\": [\n");
        prompt.append("    {\"type\": \"选择题|填空题|简答题|判断题\", \"question\": \"题干\", ");
        prompt.append("\"options\": [\"A...\", \"B...\"], \"answer\": \"参考答案\", \"score\": 20}\n");
        prompt.append("  ]\n");
        prompt.append("}\n");
        prompt.append("共 ").append(count).append(" 道题：1 道选择题、1 道填空题、1 道简答题、2 道判断题，每题 20 分。\n");
        prompt.append("选择题需给出 options，填空题与判断题无需 options。\n");
        if (domain != null && !domain.isBlank()) {
            prompt.append("领域：").append(domain).append("\n");
        }
        prompt.append("\n学习内容：\n").append(content);

        Map<String, Object> parsed = AgentJsonSupport.parseObject(
                llmService.chatStrict(prompt.toString(), ASSESS_OPTIONS));

        String topic = AgentJsonSupport.asString(parsed, "topic", "综合评估");
        List<Map<String, Object>> questions = normalizeQuestions(parsed.get("questions"), topic, count);

        Map<String, Object> assessment = new LinkedHashMap<>();
        assessment.put("topic", topic);
        assessment.put("totalQuestions", questions.size());
        assessment.put("passingScore", 60);
        assessment.put("questions", questions);
        assessment.put("instructions", "请认真作答，每题答在对应位置。完成后将答案提交至评估Agent进行评分。");
        return assessment;
    }

    /**
     * 规整题目列表：补全 id / score / answer 等字段。
     *
     * <p>若模型未返回可用题目，则退化为按题型模板生成的占位题，
     * 保证前端拿到结构完整的数据。</p>
     */
    private List<Map<String, Object>> normalizeQuestions(Object value, String topic, int count) {
        List<Map<String, Object>> raw = new ArrayList<>();
        if (value instanceof List<?> list) {
            for (Object one : list) {
                if (one instanceof Map<?, ?> map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> item = (Map<String, Object>) map;
                    raw.add(item);
                }
            }
        }

        List<Map<String, Object>> questions = new ArrayList<>();
        for (int i = 0; i < raw.size() && questions.size() < count; i++) {
            Map<String, Object> src = raw.get(i);
            if (AgentJsonSupport.asString(src, "question", "").isEmpty()) {
                continue;
            }
            Map<String, Object> q = new LinkedHashMap<>();
            q.put("id", "Q" + (questions.size() + 1));
            q.put("type", AgentJsonSupport.asString(src, "type", "简答题"));
            q.put("question", src.get("question"));
            if (src.get("options") != null) {
                q.put("options", AgentJsonSupport.asStringList(src.get("options")));
            }
            q.put("answer", AgentJsonSupport.asString(src, "answer", ""));
            q.put("score", AgentJsonSupport.asInt(src, "score", 20));
            questions.add(q);
        }

        if (!questions.isEmpty()) {
            return questions;
        }

        String[] types = {"选择题", "填空题", "简答题", "判断题", "判断题"};
        for (int i = 0; i < count; i++) {
            Map<String, Object> q = new LinkedHashMap<>();
            q.put("id", "Q" + (i + 1));
            q.put("type", types[Math.min(i, types.length - 1)]);
            q.put("question", "第" + (i + 1) + "题：【" + topic + "】相关问题");
            q.put("score", 20);
            q.put("answer", "");
            questions.add(q);
        }
        return questions;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> evaluateSubmission(List<Map<String, Object>> answers,
                                                    List<Map<String, Object>> questions) {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> questionResults = new ArrayList<>();
        double totalScore = 0;
        int maxScore = 0;

        for (Map<String, Object> answer : answers) {
            String questionId = (String) answer.get("questionId");
            String userAnswer = (String) answer.get("answer");

            Map<String, Object> qr = new LinkedHashMap<>();
            qr.put("questionId", questionId);
            qr.put("userAnswer", userAnswer);

            Map<String, Object> matchedQuestion = findQuestion(questions, questionId);
            String correctAnswer = matchedQuestion != null ? (String) matchedQuestion.get("answer") : "";
            int pointValue = matchedQuestion != null && matchedQuestion.containsKey("score")
                ? ((Number) matchedQuestion.get("score")).intValue() : 20;
            maxScore += pointValue;

            boolean isCorrect = userAnswer != null && correctAnswer != null && userAnswer.trim().equals(correctAnswer.trim());
            qr.put("correctAnswer", correctAnswer);
            qr.put("isCorrect", isCorrect);
            qr.put("score", isCorrect ? pointValue : 0);
            qr.put("maxScore", pointValue);

            if (!isCorrect && correctAnswer != null && !correctAnswer.isEmpty()) {
                qr.put("explanation", "正确答案为：" + correctAnswer + "。请复习相关知识点。");
            } else if (isCorrect) {
                qr.put("explanation", "回答正确，掌握良好！");
            }

            questionResults.add(qr);
            if (isCorrect) totalScore += pointValue;
        }

        double percentage = maxScore > 0 ? (totalScore / maxScore) * 100 : 0;
        result.put("questionResults", questionResults);
        result.put("totalScore", totalScore);
        result.put("maxScore", maxScore);
        result.put("percentage", Math.round(percentage * 10.0) / 10.0);
        result.put("passed", percentage >= 60);
        result.put("level", percentage >= 90 ? "优秀" : percentage >= 75 ? "良好" : percentage >= 60 ? "及格" : "待提高");
        result.put("feedback", generateFeedback(percentage));

        return result;
    }

    private Map<String, Object> findQuestion(List<Map<String, Object>> questions, String questionId) {
        if (questions == null || questionId == null) return null;
        for (Map<String, Object> q : questions) {
            if (questionId.equals(q.get("id")) || questionId.equals(q.get("questionId"))) return q;
        }
        return null;
    }

    private String generateFeedback(double percentage) {
        if (percentage >= 90) {
            return "优秀！您对知识掌握非常扎实，继续保持！";
        } else if (percentage >= 75) {
            return "良好！大部分知识点已掌握，建议对错题重点复习。";
        } else if (percentage >= 60) {
            String prompt = "为一个考试刚好及格的学生的学习薄弱点提出2条改进建议（50字以内）";
            return "及格。" + llmService.chat(prompt, FEEDBACK_OPTIONS);
        } else {
            String prompt = "为一个考试不及格的学生提出2条具体的学习改进建议（50字以内）";
            return "需要加强。建议：" + llmService.chat(prompt, FEEDBACK_OPTIONS);
        }
    }

    @Override
    public void updateState(AgentState state) { this.state = state; }

    @Override
    public AgentType getAgentType() { return AgentType.ASSESSOR; }

    @Override
    public AgentState getCurrentState() { return state; }

    @Override
    public List<String> getCapabilities() {
        return Arrays.asList("测评题目生成", "答案智能评分", "知识点追踪", "能力评估分析", "学习反馈生成");
    }
}
