# 多Agent协作系统设计方案

## 一、研究背景与意义

多Agent协作系统是智能教育平台的核心研究热点之一。通过设计多个专业化Agent的协作机制，可以实现复杂教学任务的分解与协调，提升个性化教育的智能化水平。

### 1.1 研究价值

- **任务分解**：将复杂学习任务分解为多个子任务，由专业化Agent处理
- **并行处理**：多个Agent并行工作，提高系统响应效率
- **专业化分工**：每个Agent专注于特定领域，提供高质量服务
- **协同决策**：Agent间通过通信协作，做出更优的教学决策

### 1.2 研究目标

本研究旨在设计并实现一个面向教育场景的多Agent协作系统，实现：
1. Agent角色的动态定义与专业化
2. Agent间的高效通信与协调机制
3. 协作策略的优化与自适应调整
4. 系统的可扩展性与容错能力

---

## 二、系统总体架构

### 2.1 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                    教育多Agent协作系统                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                   用户交互层                           │  │
│  │   用户界面 │ 任务输入 │ 结果展示 │ 反馈交互            │  │
│  └──────────────────────────────────────────────────────┘  │
│                           ↓                                 │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                  协作管理层                           │  │
│  │   任务分解 │  Agent调度 │  结果整合 │  策略优化      │  │
│  └──────────────────────────────────────────────────────┘  │
│                           ↓                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │  规划Agent  │  │  教学Agent  │  │  评估Agent  │         │
│  │   Planner   │  │   Tutor    │  │  Assessor  │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
│         ↓                ↓                ↓                │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │  答疑Agent  │  │  推荐Agent  │  │  监督Agent  │         │
│  │    QA       │  │  Recommend  │  │  Monitor    │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
│                           ↓                                 │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                    共享知识库                         │  │
│  │   用户状态 │  课程知识 │  历史交互 │  系统配置       │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 Agent角色定义

| Agent类型 | 核心职责 | 输入 | 输出 |
|----------|---------|------|------|
| **规划Agent** | 学习目标分解、任务规划 | 用户需求、学习目标 | 学习计划、任务列表 |
| **教学Agent** | 知识讲解、内容呈现 | 学习任务、知识主题 | 教学内容、讲解材料 |
| **评估Agent** | 学习效果评估、测评生成 | 学习行为、答题数据 | 评估报告、能力画像 |
| **答疑Agent** | 问题解答、困惑处理 | 用户问题、知识库 | 答案、解释、引导 |
| **推荐Agent** | 学习资源推荐、路径优化 | 用户画像、知识图谱 | 推荐列表、学习建议 |
| **监督Agent** | 学习行为监控、异常检测 | 学习行为数据 | 监督报告、干预建议 |

---

## 三、核心模块详细设计

### 3.1 Agent基础框架

```java
/**
 * Agent基础接口
 * 定义所有Agent的通用行为和方法
 */
public interface BaseAgent {
    
    /**
     * Agent处理任务
     * @param task 输入任务
     * @return 任务执行结果
     */
    AgentResult processTask(Task task);
    
    /**
     * Agent状态更新
     * @param state 新状态
     */
    void updateState(AgentState state);
    
    /**
     * 获取Agent类型
     * @return Agent类型枚举
     */
    AgentType getAgentType();
    
    /**
     * 获取Agent能力描述
     * @return 能力列表
     */
    List<String> getCapabilities();
}

/**
 * Agent状态枚举
 */
public enum AgentState {
    IDLE,           // 空闲状态
    PROCESSING,     // 处理中
    WAITING,        // 等待中
    COLLABORATING,  // 协作中
    ERROR,          // 错误状态
    TERMINATED      // 已终止
}

/**
 * Agent类型枚举
 */
public enum AgentType {
    PLANNER,       // 规划Agent
    TUTOR,         // 教学Agent
    ASSESSOR,      // 评估Agent
    QA,            // 答疑Agent
    RECOMMENDER,   // 推荐Agent
    MONITOR        // 监督Agent
}
```

### 3.2 Agent通信机制

```java
/**
 * Agent消息类
 * 用于Agent间的信息传递
 */
public class AgentMessage {
    private String messageId;          // 消息ID
    private AgentType sender;          // 发送者
    private AgentType receiver;        // 接收者
    private MessageType type;          // 消息类型
    private Object content;            // 消息内容
    private Map<String, Object> metadata; // 元数据
    private long timestamp;            // 时间戳
    
    // Getters and Setters
}

/**
 * 消息类型枚举
 */
public enum MessageType {
    REQUEST,        // 请求消息
    RESPONSE,       // 响应消息
    QUERY,          // 查询消息
    NOTIFY,         // 通知消息
    COLLABORATE,    // 协作消息
    FEEDBACK,       // 反馈消息
    TERMINATE       // 终止消息
}

/**
 * 消息队列管理器
 */
@Component
public class AgentMessageQueue {
    
    private final Map<AgentType, BlockingQueue<AgentMessage>> queues;
    private final AgentMessageBus messageBus;
    
    /**
     * 发送消息到指定Agent
     */
    public void sendMessage(AgentMessage message) {
        AgentType receiver = message.getReceiver();
        BlockingQueue<AgentMessage> queue = queues.get(receiver);
        if (queue != null) {
            queue.offer(message);
            messageBus.publish(message);
        }
    }
    
    /**
     * 获取消息
     */
    public AgentMessage receiveMessage(AgentType agentType, long timeout) {
        BlockingQueue<AgentMessage> queue = queues.get(agentType);
        try {
            return queue.poll(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
}
```

### 3.3 任务分解与分配

```java
/**
 * 任务分解器
 * 将复杂任务分解为可执行的子任务
 */
@Service
public class TaskDecomposer {
    
    /**
     * 分解学习任务
     * @param originalTask 原始任务
     * @return 子任务列表
     */
    public List<SubTask> decomposeTask(Task originalTask) {
        // 1. 分析任务类型和复杂度
        TaskAnalysis analysis = analyzeTask(originalTask);
        
        // 2. 确定需要的Agent类型
        List<AgentType> requiredAgents = determineRequiredAgents(analysis);
        
        // 3. 生成子任务
        List<SubTask> subTasks = generateSubTasks(originalTask, requiredAgents);
        
        // 4. 建立任务依赖关系
        buildTaskDependencies(subTasks);
        
        // 5. 优化任务分配
        return optimizeTaskAllocation(subTasks);
    }
    
    /**
     * 任务分析
     */
    private TaskAnalysis analyzeTask(Task task) {
        TaskAnalysis analysis = new TaskAnalysis();
        analysis.setComplexity(evaluateComplexity(task));
        analysis.setDomain(task.getDomain());
        analysis.setRequiredSkills(extractRequiredSkills(task));
        analysis.setEstimatedDuration(estimateDuration(task));
        return analysis;
    }
    
    /**
     * 确定所需Agent
     */
    private List<AgentType> determineRequiredAgents(TaskAnalysis analysis) {
        List<AgentType> agents = new ArrayList<>();
        
        // 总是需要教学Agent
        agents.add(AgentType.TUTOR);
        
        // 根据任务类型添加其他Agent
        if (analysis.hasAssessment()) {
            agents.add(AgentType.ASSESSOR);
        }
        if (analysis.hasQuestions()) {
            agents.add(AgentType.QA);
        }
        if (analysis.needsResources()) {
            agents.add(AgentType.RECOMMENDER);
        }
        if (analysis.requiresPlanning()) {
            agents.add(AgentType.PLANNER);
        }
        if (analysis.needsMonitoring()) {
            agents.add(AgentType.MONITOR);
        }
        
        return agents;
    }
    
    /**
     * 生成子任务
     */
    private List<SubTask> generateSubTasks(Task task, List<AgentType> agents) {
        List<SubTask> subTasks = new ArrayList<>();
        
        for (AgentType agent : agents) {
            SubTask subTask = new SubTask();
            subTask.setId(UUID.randomUUID().toString());
            subTask.setAssignedAgent(agent);
            subTask.setParentTaskId(task.getId());
            subTask.setPriority(calculatePriority(agent, task));
            subTask.setInputSpec(defineInputSpec(agent, task));
            subTask.setOutputSpec(defineOutputSpec(agent, task));
            
            subTasks.add(subTask);
        }
        
        return subTasks;
    }
    
    /**
     * 构建任务依赖关系
     */
    private void buildTaskDependencies(List<SubTask> subTasks) {
        // 规划Agent先执行
        // 教学Agent依赖规划结果
        // 评估Agent依赖教学完成
        // ...
    }
    
    /**
     * 优化任务分配
     */
    private List<SubTask> optimizeTaskAllocation(List<SubTask> subTasks) {
        // 按优先级排序
        subTasks.sort(Comparator.comparing(SubTask::getPriority).reversed());
        return subTasks;
    }
}

/**
 * 任务分配器
 */
@Service
public class TaskAllocator {
    
    private final Map<AgentType, Agent> agentRegistry;
    private final LoadBalancer loadBalancer;
    
    /**
     * 分配任务到Agent
     */
    public AllocationResult allocateTask(SubTask task) {
        AgentType targetAgent = task.getAssignedAgent();
        
        // 获取该类型的所有Agent实例
        List<Agent> availableAgents = agentRegistry.getAgents(targetAgent);
        
        // 使用负载均衡选择最佳Agent
        Agent selectedAgent = loadBalancer.select(availableAgents, task);
        
        // 分配任务
        AllocationResult result = new AllocationResult();
        result.setTask(task);
        result.setAssignedAgent(selectedAgent);
        result.setEstimatedTime(predictExecutionTime(selectedAgent, task));
        
        return result;
    }
    
    /**
     * 批量分配任务
     */
    public List<AllocationResult> allocateTasks(List<SubTask> tasks) {
        return tasks.stream()
            .map(this::allocateTask)
            .collect(Collectors.toList());
    }
}
```

---

## 四、Agent核心实现

### 4.1 规划Agent实现

```java
/**
 * 规划Agent
 * 负责学习目标分解和任务规划
 */
@Service
public class PlannerAgent implements BaseAgent {
    
    private AgentState state = AgentState.IDLE;
    private final KnowledgeGraphService knowledgeGraph;
    private final UserModelService userModel;
    
    @Override
    public AgentResult processTask(Task task) {
        updateState(AgentState.PROCESSING);
        
        try {
            // 1. 理解学习目标
            LearningGoal goal = parseLearningGoal(task);
            
            // 2. 分析当前状态
            LearnerState currentState = userModel.getCurrentState(task.getUserId());
            
            // 3. 构建知识依赖图
            DependencyGraph depGraph = knowledgeGraph.buildDependency(goal);
            
            // 4. 规划学习路径
            LearningPath path = planLearningPath(goal, currentState, depGraph);
            
            // 5. 生成任务列表
            List<LearningTask> taskList = generateTaskList(path);
            
            // 6. 估计时间和资源
            PlanMetadata metadata = estimateResources(taskList);
            
            // 构建结果
            AgentResult result = new AgentResult();
            result.setSuccess(true);
            result.setData(buildPlanResponse(taskList, metadata));
            
            return result;
            
        } catch (Exception e) {
            return handleError(e);
        } finally {
            updateState(AgentState.IDLE);
        }
    }
    
    /**
     * 解析学习目标
     */
    private LearningGoal parseLearningGoal(Task task) {
        // 提取目标关键词
        String goalText = task.getContent();
        
        // 使用NLP解析目标
        LearningGoal goal = new LearningGoal();
        goal.setId(UUID.randomUUID().toString());
        goal.setDescription(goalText);
        goal.setTargetConcepts(extractConcepts(goalText));
        goal.setProficiencyLevel(determineLevel(goalText));
        goal.setDeadline(task.getDeadline());
        
        return goal;
    }
    
    /**
     * 规划学习路径
     */
    private LearningPath planLearningPath(
            LearningGoal goal,
            LearnerState currentState,
            DependencyGraph depGraph) {
        
        LearningPath path = new LearningPath();
        path.setId(UUID.randomUUID().toString());
        
        // 获取目标知识点
        List<String> targetConcepts = goal.getTargetConcepts();
        
        // 获取已掌握知识点
        Set<String> masteredConcepts = currentState.getMasteredConcepts();
        
        // 确定需要学习的知识点
        List<String> toLearnConcepts = targetConcepts.stream()
            .filter(c -> !masteredConcepts.contains(c))
            .collect(Collectors.toList());
        
        // 拓扑排序确定学习顺序
        List<String> learningOrder = topologicalSort(toLearnConcepts, depGraph);
        
        // 构建学习路径
        List<PathNode> nodes = new ArrayList<>();
        int week = 1;
        
        for (String concept : learningOrder) {
            PathNode node = new PathNode();
            node.setConcept(concept);
            node.setEstimatedWeeks(estimateLearningTime(concept));
            node.setStartWeek(week);
            node.setEndWeek(week + node.getEstimatedWeeks() - 1);
            node.setPrerequisites(depGraph.getPrerequisites(concept));
            node.setRelatedConcepts(depGraph.getRelatedConcepts(concept));
            
            nodes.add(node);
            week += node.getEstimatedWeeks();
        }
        
        path.setNodes(nodes);
        path.setTotalWeeks(week - 1);
        
        return path;
    }
    
    /**
     * 生成任务列表
     */
    private List<LearningTask> generateTaskList(LearningPath path) {
        List<LearningTask> tasks = new ArrayList<>();
        
        for (PathNode node : path.getNodes()) {
            // 为每个知识点生成多个任务
            LearningTask lectureTask = createLectureTask(node);
            LearningTask practiceTask = createPracticeTask(node);
            LearningTask reviewTask = createReviewTask(node);
            
            tasks.add(lectureTask);
            tasks.add(practiceTask);
            tasks.add(reviewTask);
        }
        
        return tasks;
    }
    
    @Override
    public void updateState(AgentState state) {
        this.state = state;
    }
    
    @Override
    public AgentType getAgentType() {
        return AgentType.PLANNER;
    }
    
    @Override
    public List<String> getCapabilities() {
        return Arrays.asList(
            "学习目标分析",
            "知识依赖分析",
            "学习路径规划",
            "任务分解生成",
            "时间资源估算"
        );
    }
}
```

### 4.2 教学Agent实现

```java
/**
 * 教学Agent
 * 负责知识讲解和内容呈现
 */
@Service
public class TutorAgent implements BaseAgent {
    
    private AgentState state = AgentState.IDLE;
    private final ContentService contentService;
    private final MultimodalGenerator multimodalGenerator;
    
    @Override
    public AgentResult processTask(Task task) {
        updateState(AgentState.PROCESSING);
        
        try {
            // 1. 解析教学任务
            TeachingTask teachingTask = parseTeachingTask(task);
            
            // 2. 获取教学内容
            LearningContent content = contentService.getContent(
                teachingTask.getConcept()
            );
            
            // 3. 适应学习者特征
            ContentAdapter adapter = new ContentAdapter();
            LearningContent adaptedContent = adapter.adapt(
                content,
                teachingTask.getLearnerProfile()
            );
            
            // 4. 生成多模态教学材料
            TeachingMaterial material = generateTeachingMaterial(
                adaptedContent,
                teachingTask.getPreferences()
            );
            
            // 5. 构建教学响应
            AgentResult result = new AgentResult();
            result.setSuccess(true);
            result.setData(buildTeachingResponse(material));
            
            return result;
            
        } catch (Exception e) {
            return handleError(e);
        } finally {
            updateState(AgentState.IDLE);
        }
    }
    
    /**
     * 生成教学材料
     */
    private TeachingMaterial generateTeachingMaterial(
            LearningContent content,
            LearningPreferences prefs) {
        
        TeachingMaterial material = new TeachingMaterial();
        
        // 根据偏好生成不同形式的内容
        if (prefs.prefersText()) {
            material.setTextExplanation(generateTextExplanation(content));
        }
        
        if (prefs.prefersVisual()) {
            material.setVisualContent(generateVisualContent(content));
        }
        
        if (prefs.prefersAudio()) {
            material.setAudioExplanation(generateAudioExplanation(content));
        }
        
        if (prefs.prefersInteractive()) {
            material.setInteractiveElements(createInteractiveElements(content));
        }
        
        // 添加示例和练习
        material.setExamples(generateExamples(content));
        material.setExercises(generateExercises(content));
        
        // 添加知识检查点
        material.setCheckpoints(generateCheckpoints(content));
        
        return material;
    }
    
    /**
     * 生成文本解释
     */
    private String generateTextExplanation(LearningContent content) {
        // 使用模板生成结构化解释
        StringBuilder explanation = new StringBuilder();
        
        // 1. 概念定义
        explanation.append("# ").append(content.getConceptName()).append("\n\n");
        explanation.append(content.getDefinition()).append("\n\n");
        
        // 2. 核心要点
        explanation.append("## 核心要点\n\n");
        for (String keyPoint : content.getKeyPoints()) {
            explanation.append("- ").append(keyPoint).append("\n");
        }
        explanation.append("\n");
        
        // 3. 详细解释
        explanation.append("## 详细说明\n\n");
        explanation.append(content.getDetailedExplanation()).append("\n\n");
        
        // 4. 常见误解
        explanation.append("## 常见误解\n\n");
        for (Misconception mis : content.getMisconceptions()) {
            explanation.append("❌ ").append(mis.getWrong()).append("\n");
            explanation.append("✅ ").append(mis.getCorrect()).append("\n\n");
        }
        
        return explanation.toString();
    }
    
    /**
     * 生成可视化内容
     */
    private VisualContent generateVisualContent(LearningContent content) {
        VisualContent visual = new VisualContent();
        
        // 生成知识图谱可视化
        if (content.hasGraphStructure()) {
            visual.setKnowledgeGraph(
                visualizeKnowledgeGraph(content.getGraph())
            );
        }
        
        // 生成流程图
        if (content.isProcedural()) {
            visual.setFlowchart(
                generateFlowchart(content.getProcedure())
            );
        }
        
        // 生成对比图表
        if (content.hasComparisons()) {
            visual.setComparisonChart(
                generateComparisonChart(content.getComparisons())
            );
        }
        
        return visual;
    }
    
    @Override
    public void updateState(AgentState state) {
        this.state = state;
    }
    
    @Override
    public AgentType getAgentType() {
        return AgentType.TUTOR;
    }
    
    @Override
    public List<String> getCapabilities() {
        return Arrays.asList(
            "知识讲解",
            "多模态内容生成",
            "学习者适配",
            "示例生成",
            "练习设计"
        );
    }
}
```

### 4.3 评估Agent实现

```java
/**
 * 评估Agent
 * 负责学习效果评估和能力测评
 */
@Service
public class AssessorAgent implements BaseAgent {
    
    private AgentState state = AgentState.IDLE;
    private final QuestionBankService questionBank;
    private final KnowledgeTracer knowledgeTracer;
    private final AbilityAnalyzer abilityAnalyzer;
    
    @Override
    public AgentResult processTask(Task task) {
        updateState(AgentState.PROCESSING);
        
        try {
            // 1. 解析评估任务
            AssessmentTask assessTask = parseAssessmentTask(task);
            
            // 2. 选择评估策略
            AssessmentStrategy strategy = selectStrategy(assessTask);
            
            // 3. 生成评估内容
            AssessmentContent assessment = generateAssessment(
                assessTask,
                strategy
            );
            
            // 4. 或评估已有答案
            if (assessTask.hasSubmission()) {
                AssessmentResult result = evaluateSubmission(
                    assessTask,
                    assessment
                );
                
                // 5. 更新知识追踪
                updateKnowledgeTrace(assessTask.getUserId(), result);
                
                AgentResult agentResult = new AgentResult();
                agentResult.setSuccess(true);
                agentResult.setData(buildAssessmentResponse(result));
                
                return agentResult;
            }
            
            // 返回评估内容
            AgentResult result = new AgentResult();
            result.setSuccess(true);
            result.setData(buildAssessmentContent(assessment));
            
            return result;
            
        } catch (Exception e) {
            return handleError(e);
        } finally {
            updateState(AgentState.IDLE);
        }
    }
    
    /**
     * 生成评估内容
     */
    private AssessmentContent generateAssessment(
            AssessmentTask task,
            AssessmentStrategy strategy) {
        
        AssessmentContent content = new AssessmentContent();
        
        // 根据策略确定题目组合
        int diagnosticCount = (int)(strategy.getTotalQuestions() * 0.2);
        int masteryCount = (int)(strategy.getTotalQuestions() * 0.5);
        int applicationCount = (int)(strategy.getTotalQuestions() * 0.3);
        
        // 1. 诊断性题目 - 检测前备知识
        content.setDiagnosticQuestions(
            questionBank.selectQuestions(
                QuestionType.DIAGNOSTIC,
                diagnosticCount,
                task.getPrerequisites()
            )
        );
        
        // 2. 掌握性题目 - 检测目标知识
        content.setMasteryQuestions(
            questionBank.selectQuestions(
                QuestionType.MASTERY,
                masteryCount,
                task.getTargetConcepts()
            )
        );
        
        // 3. 应用性题目 - 检测知识应用
        content.setApplicationQuestions(
            questionBank.selectQuestions(
                QuestionType.APPLICATION,
                applicationCount,
                task.getTargetConcepts()
            )
        );
        
        // 设置评估参数
        content.setTimeLimit(strategy.getTimeLimit());
        content.setPassingScore(strategy.getPassingScore());
        content.setFeedbackLevel(strategy.getFeedbackLevel());
        
        return content;
    }
    
    /**
     * 评估提交答案
     */
    private AssessmentResult evaluateSubmission(
            AssessmentTask task,
            AssessmentContent content) {
        
        AssessmentResult result = new AssessmentResult();
        result.setUserId(task.getUserId());
        result.setAssessmentId(content.getId());
        
        // 逐题评分
        List<QuestionResult> questionResults = new ArrayList<>();
        double totalScore = 0;
        
        for (SubmissionAnswer answer : task.getAnswers()) {
            QuestionResult qr = evaluateQuestion(answer, content);
            questionResults.add(qr);
            totalScore += qr.getScore();
        }
        
        result.setQuestionResults(questionResults);
        result.setTotalScore(totalScore);
        result.setMaxScore(content.getTotalScore());
        result.setPercentage((totalScore / content.getTotalScore()) * 100);
        
        // 知识点掌握度分析
        result.setConceptMastery(analyzeConceptMastery(questionResults));
        
        // 能力维度分析
        result.setAbilityProfile(abilityAnalyzer.analyze(questionResults));
        
        // 生成反馈
        result.setFeedback(generateFeedback(result, content));
        
        return result;
    }
    
    /**
     * 更新知识追踪
     */
    private void updateKnowledgeTrace(String userId, AssessmentResult result) {
        // 使用BKT或DKT模型更新知识状态
        for (Map.Entry<String, Double> entry : 
                result.getConceptMastery().entrySet()) {
            
            KnowledgeState state = new KnowledgeState();
            state.setUserId(userId);
            state.setConcept(entry.getKey());
            state.setMasteryProbability(entry.getValue());
            state.setTimestamp(System.currentTimeMillis());
            
            knowledgeTracer.update(state);
        }
    }
    
    @Override
    public void updateState(AgentState state) {
        this.state = state;
    }
    
    @Override
    public AgentType getAgentType() {
        return AgentType.ASSESSOR;
    }
    
    @Override
    public List<String> getCapabilities() {
        return Arrays.asList(
            "测评题目生成",
            "答案智能评分",
            "知识点追踪",
            "能力评估分析",
            "学习反馈生成"
        );
    }
}
```

---

## 五、协作策略优化

### 5.1 协作流程设计

```java
/**
 * 协作管理器
 * 协调多个Agent的工作
 */
@Service
public class CollaborationManager {
    
    private final AgentRegistry agentRegistry;
    private final TaskDecomposer decomposer;
    private final TaskAllocator allocator;
    private final ResultAggregator aggregator;
    private final StrategyOptimizer optimizer;
    
    /**
     * 执行协作任务
     */
    public CollaborationResult executeTask(Task task) {
        // 1. 任务分解
        List<SubTask> subTasks = decomposer.decomposeTask(task);
        
        // 2. 任务分配
        List<AllocationResult> allocations = allocator.allocateTasks(subTasks);
        
        // 3. 并行执行（依赖关系允许的情况下）
        List<Future<AgentResult>> futures = executeInParallel(allocations);
        
        // 4. 收集结果
        List<AgentResult> results = collectResults(futures);
        
        // 5. 结果整合
        CollaborationResult collaboration = aggregator.aggregate(results);
        
        // 6. 策略优化
        optimizer.optimizeStrategy(task, collaboration);
        
        return collaboration;
    }
    
    /**
     * 并行执行任务
     */
    private List<Future<AgentResult>> executeInParallel(
            List<AllocationResult> allocations) {
        
        ExecutorService executor = Executors.newFixedThreadPool(
            Math.min(allocations.size(), Runtime.getRuntime().availableProcessors())
        );
        
        List<Future<AgentResult>> futures = allocations.stream()
            .map(allocation -> {
                return executor.submit(() -> {
                    BaseAgent agent = agentRegistry.getAgent(
                        allocation.getAssignedAgent().getType()
                    );
                    return agent.processTask(allocation.getTask());
                });
            })
            .collect(Collectors.toList());
        
        return futures;
    }
    
    /**
     * 收集结果
     */
    private List<AgentResult> collectResults(
            List<Future<AgentResult>> futures) {
        
        List<AgentResult> results = new ArrayList<>();
        
        for (Future<AgentResult> future : futures) {
            try {
                results.add(future.get(30, TimeUnit.SECONDS));
            } catch (Exception e) {
                // 处理超时或执行失败
                results.add(createErrorResult(e));
            }
        }
        
        return results;
    }
}

/**
 * 结果聚合器
 */
@Service
public class ResultAggregator {
    
    /**
     * 聚合多Agent结果
     */
    public CollaborationResult aggregate(List<AgentResult> results) {
        CollaborationResult collaboration = new CollaborationResult();
        
        // 1. 分类整理结果
        Map<AgentType, AgentResult> categorized = categorize(results);
        
        // 2. 提取关键信息
        collaboration.setPlanResult(categorized.get(AgentType.PLANNER));
        collaboration.setTeachingResults(
            extractTeachingContent(categorized.get(AgentType.TUTOR))
        );
        collaboration.setAssessmentResult(
            categorized.get(AgentType.ASSESSOR)
        );
        collaboration.setRecommendations(
            extractRecommendations(categorized.get(AgentType.RECOMMENDER))
        );
        
        // 3. 一致性检查
        checkConsistency(collaboration);
        
        // 4. 生成综合报告
        collaboration.setSummary(generateSummary(collaboration));
        
        return collaboration;
    }
}
```

### 5.2 强化学习优化策略

```python
# 协作策略优化器 (Python)
import numpy as np
from collections import defaultdict

class CollaborationOptimizer:
    """
    基于强化学习的多Agent协作策略优化器
    """
    
    def __init__(self, state_dim, action_dim, learning_rate=0.001):
        self.state_dim = state_dim
        self.action_dim = action_dim
        
        # Q网络
        self.q_network = self._build_network()
        self.target_network = self._build_network()
        
        # 经验回放
        self.memory = ReplayBuffer(capacity=10000)
        
        # 超参数
        self.gamma = 0.99  # 折扣因子
        self.epsilon = 1.0  # 探索率
        self.epsilon_decay = 0.995
        self.epsilon_min = 0.01
        self.learning_rate = learning_rate
        
        # 策略记录
        self策略历史 = []
        
    def _build_network(self):
        """构建Q网络"""
        model = tf.keras.Sequential([
            layers.Dense(128, activation='relu', input_shape=(self.state_dim,)),
            layers.Dense(128, activation='relu'),
            layers.Dense(64, activation='relu'),
            layers.Dense(self.action_dim, activation='linear')
        ])
        model.compile(optimizer=Adam(self.learning_rate), loss='mse')
        return model
    
    def get_action(self, state):
        """
        选择协作动作
        动作包括: Agent类型选择、任务分配、协作模式等
        """
        if np.random.random() < self.epsilon:
            # 探索: 随机选择
            return np.random.randint(self.action_dim)
        
        # 利用: 选择Q值最大的动作
        state = np.reshape(state, [1, self.state_dim])
        q_values = self.q_network.predict(state, verbose=0)
        return np.argmax(q_values[0])
    
    def remember(self, state, action, reward, next_state, done):
        """存储经验"""
        self.memory.push(state, action, reward, next_state, done)
    
    def replay(self, batch_size):
        """经验回放训练"""
        if len(self.memory) < batch_size:
            return
        
        # 采样
        states, actions, rewards, next_states, dones = \
            self.memory.sample(batch_size)
        
        # 计算目标Q值
        target_q = self.q_network.predict(states, verbose=0)
        next_target_q = self.target_network.predict(next_states, verbose=0)
        
        for i in range(batch_size):
            if dones[i]:
                target_q[i][actions[i]] = rewards[i]
            else:
                target_q[i][actions[i]] = rewards[i] + \
                    self.gamma * np.max(next_target_q[i])
        
        # 更新网络
        self.q_network.fit(states, target_q, epochs=1, verbose=0)
    
    def update_target_network(self):
        """更新目标网络"""
        self.target_network.set_weights(self.q_network.get_weights())
    
    def calculate_reward(self, collaboration_result):
        """
        计算协作奖励
        综合考虑: 效率、质量、用户满意度
        """
        # 效率奖励: 响应时间越短越好
        efficiency_reward = -0.1 * collaboration_result.get_response_time()
        
        # 质量奖励: 评估结果准确性
        quality_reward = 0.5 * collaboration_result.get_accuracy()
        
        # 一致性奖励: Agent间协作效果
        consistency_reward = 0.3 * collaboration_result.get_consistency_score()
        
        # 用户满意度奖励
        satisfaction_reward = 0.2 * collaboration_result.get_user_satisfaction()
        
        total_reward = (efficiency_reward + quality_reward + 
                       consistency_reward + satisfaction_reward)
        
        return total_reward
```

---

## 六、共享知识库设计

### 6.1 知识库架构

```java
/**
 * 共享知识库服务
 * 为所有Agent提供共享状态和知识
 */
@Service
public class SharedKnowledgeBase {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final GraphDatabaseService graphDatabase;
    private final UserModelRepository userModelRepo;
    
    /**
     * 存储用户状态
     */
    public void saveUserState(String userId, UserState state) {
        String key = "user:state:" + userId;
        redisTemplate.opsForValue().set(key, state, Duration.ofHours(24));
    }
    
    /**
     * 获取用户状态
     */
    public UserState getUserState(String userId) {
        String key = "user:state:" + userId;
        return (UserState) redisTemplate.opsForValue().get(key);
    }
    
    /**
     * 添加会话历史
     */
    public void addConversationHistory(String userId, Conversation conv) {
        String key = "user:conversation:" + userId;
        redisTemplate.opsForList().rightPush(key, conv);
    }
    
    /**
     * 查询相关知识
     */
    public List<KnowledgeEntry> queryKnowledge(String query) {
        // 混合检索: 关键词 + 向量
        List<KnowledgeEntry> results = new ArrayList<>();
        
        // 1. 关键词检索
        results.addAll(keywordSearch(query));
        
        // 2. 向量检索
        results.addAll(vectorSearch(query));
        
        // 3. 知识图谱检索
        results.addAll(graphSearch(query));
        
        // 4. 去重和排序
        return deduplicateAndRank(results);
    }
    
    /**
     * 更新知识图谱
     */
    public void updateKnowledgeGraph(String concept, 
                                     Map<String, Object> updates) {
        String cypher = """
            MATCH (c:Concept {name: $concept})
            SET c += $properties
            """;
        
        Map<String, Object> params = new HashMap<>();
        params.put("concept", concept);
        params.put("properties", updates);
        
        graphDatabase.execute(cypher, params);
    }
}
```

---

## 七、实验设计与评估

### 7.1 评估指标

| 指标类别 | 指标名称 | 计算方法 | 权重 |
|---------|---------|---------|------|
| **效率指标** | 响应时间 | 平均任务完成时间 | 0.2 |
| | 并行度 | 有效并行任务比例 | 0.1 |
| **质量指标** | 准确性 | 任务完成准确率 | 0.3 |
| | 完整性 | 覆盖所有需求程度 | 0.2 |
| **协作指标** | 一致性 | Agent输出一致性 | 0.1 |
| | 协调性 | 任务分配合理性 | 0.1 |

### 7.2 对比实验设计

```python
# 实验配置
experiments = {
    # 实验1: 多Agent vs 单Agent
    "exp1_multi_vs_single": {
        "baseline": "单一大模型Agent",
        "treatment": "多Agent协作系统",
        "metrics": ["accuracy", "response_time", "user_satisfaction"]
    },
    
    # 实验2: 不同协作策略
    "exp2_collaboration_strategies": {
        "treatments": [
            "固定角色分配",
            "动态角色调整",
            "强化学习优化策略"
        ],
        "metrics": ["coordination_score", "task_completion_rate"]
    },
    
    # 实验3: Agent数量影响
    "exp3_agent_scaling": {
        "treatments": [
            "2个Agent",
            "4个Agent", 
            "6个Agent",
            "8个Agent"
        ],
        "metrics": ["performance", "efficiency", "resource_usage"]
    }
}
```

---

## 八、实现步骤与时间规划

### 第一阶段：基础框架搭建（4周）

| 周次 | 任务 | 产出 |
|------|------|------|
| 1 | Agent基础框架设计 | 接口定义、抽象类 |
| 2 | 消息通信机制实现 | 消息队列、事件总线 |
| 3 | 任务分解器开发 | 任务分解算法 |
| 4 | 任务分配器开发 | 负载均衡策略 |

### 第二阶段：核心Agent开发（6周）

| 周次 | 任务 | 产出 |
|------|------|------|
| 5-6 | 规划Agent实现 | 路径规划算法 |
| 7-8 | 教学Agent实现 | 内容生成模块 |
| 9-10 | 评估Agent实现 | 知识追踪模型 |

### 第三阶段：协作机制完善（4周）

| 周次 | 任务 | 产出 |
|------|------|------|
| 11 | 协作流程优化 | 协作管理器 |
| 12 | 共享知识库搭建 | Redis缓存、知识图谱 |
| 13 | 策略优化模块 | 强化学习优化器 |
| 14 | 系统集成测试 | 完整系统 |

### 第四阶段：实验验证（4周）

| 周次 | 任务 | 产出 |
|------|------|------|
| 15 | 对比实验 | 实验数据 |
| 16 | 消融实验 | 贡献度分析 |
| 17 | 用户实验 | 用户反馈 |
| 18 | 结果分析 | 论文素材 |

---

## 九、预期成果

### 9.1 系统成果

1. **完整的多Agent协作系统**
   - 支持6种专业化Agent
   - 高效的协作通信机制
   - 智能的任务分解与分配
   - 自适应的策略优化

2. **可复用的框架**
   - Agent开发模板
   - 协作模式库
   - 评估基准套件

### 9.2 学术成果

1. **论文发表**
   - 核心算法论文1篇
   - 系统论文1篇

2. **创新贡献**
   - 提出教育领域多Agent协作框架
   - 设计自适应协作策略优化方法
   - 实现可解释的Agent协调机制

---

## 十、参考资源

### 论文

1. "MetaGPT: Meta Programming for Multi-Agent Collaboration"
2. "ChatDev: Interactive Software Development via Multi-Agent Collaboration"
3. "Multi-Agent Reinforcement Learning: A Survey"
4. "Cooperative Multi-Agent Learning for Task Completion"

### 工具框架

1. LangChain Agents
2. AutoGen (Microsoft)
3. CrewAI
4. LangGraph

---

## 十一、面向 DeepSeek 的可落地实施指南

> 前十节是**设计/论文层**（架构、算法、思路），偏抽象且含大量 `// ...` 伪代码、以及本项目并不具备的组件（Redis、Neo4j、Python 强化学习）。
> 本节是**工程执行层**：把上面的蓝图**校准到本仓库的真实技术栈**，并拆成 DeepSeek 可以逐步执行、每步都能编译运行、每步都有验收标准的实现规格。**DeepSeek 请以本节为准；与前十节冲突时，以本节的技术选型为准。**

### 11.0 给 DeepSeek 的总则（务必先读）

**你的角色**：在既有 Spring Boot 项目 `houduan/backend` 内，新增一个多 Agent 协作子系统，复用现有能力，不破坏现有功能。

**硬约束（违反即返工）**：
1. **只用现有技术栈**：Java 17 + Spring Boot 3.5.13 + Spring Data MongoDB + 现有 `GlmApiService`（GLM `glm-4.7`）+ Vue 3 前端。**禁止**引入 Redis、Neo4j、Milvus、Kafka、Python、LangChain/AutoGen 等新中间件或新语言——除非某步骤明确批注"可选增量"。蓝图里出现的这些组件，一律按 §11.1 的映射替换为现有实现。
2. **禁止空实现**：不允许提交 `// TODO`、`// ...`、`return null;` 占位的方法。每个方法要么完整实现，要么本步骤不创建它。
3. **先编译后推进**：每完成一个阶段，必须 `cd houduan/backend && ./mvnw -q -DskipTests compile` 通过，再进入下一阶段。不通过就地修复，不要堆积。
4. **结构化输出契约**：Agent 与 GLM 的交互一律"要求模型返回 JSON + 代码侧健壮解析 + 失败降级"，不要用正则从自由文本里抠字段。
5. **幂等与隔离**：新增代码放在独立包 `com.edu.backend.agent` 下，不改动 `controller/AgentController.java` 已有端点（新增独立的 `AgentCollaborationController`）。
6. **不改动数据库已有集合的结构**；新集合以 `agent_` 前缀命名。

**产物总览**（最终形态）：
```
com.edu.backend.agent
├── model/            # Task, SubTask, AgentResult, AgentContext, CollaborationResult, 枚举
├── persistence/      # MongoDB 实体(agent_runs / agent_messages / agent_context) + Repository
├── core/             # AbstractGlmAgent 基类, AgentType 枚举, JSON 解析工具
├── agents/           # PlannerAgent / TutorAgent / AssessorAgent / QaAgent / RecommenderAgent / MonitorAgent
├── orchestration/    # TaskDecomposer, CollaborationOrchestrator, ResultAggregator
├── context/          # SharedContextService(替代 Redis/Neo4j)
└── web/              # AgentCollaborationController + 请求/响应 DTO
```

---

### 11.1 蓝图 → 真实项目 的对接映射（现实校准表）

| 蓝图中的组件 | 蓝图设想 | **本项目落地方案** |
|---|---|---|
| LLM 后端 | 泛指大模型 | 现有 `GlmApiService`（需按 §11.2 增强），模型 `glm-4.7` |
| 共享状态缓存 | Redis `RedisTemplate` | **MongoDB `agent_context` 集合 + 进程内 `ConcurrentHashMap` 短期缓存**（`SharedContextService`） |
| 知识图谱 | Neo4j `GraphDatabaseService` / Cypher | **复用现有 `RAGKnowledgeBaseService.enhancedQA()`**（向量+BM25 混合检索），不引入图数据库 |
| 向量/关键词检索 | 自建 | 现有 `RAGKnowledgeBaseService` 已实现，直接注入调用 |
| 强化学习策略优化（DQN/Python） | 在线 RL | **默认用"规则 + 加权评分"启发式**（`HeuristicStrategy`）；RL 列为**可选研究增量**（§11.7 阶段5），毕设够用即可，不阻塞主线 |
| Agent 注册表 / 负载均衡 | `LoadBalancer` 多实例 | 单实例：Spring 容器天然管理 Bean，`AgentRegistry` 用 `Map<AgentType, AbstractGlmAgent>` 由 Spring 自动注入即可，**不做多实例负载均衡** |
| 消息队列 `BlockingQueue` | Agent 异步互发消息 | **同步编排优先**：`CollaborationOrchestrator` 直接按 DAG 顺序/并行调用 Agent；消息仅落库 `agent_messages` 供追溯/前端展示，不做真正的 MQ |
| 用户模型 `UserModelService` | 独立服务 | 复用现有 `users` / `user_progress` / `user_grades` 集合（`MongoTemplate` 查询） |

> **一句话**：本系统 = "**一个编排器 + 六个基于 GLM 的专业 Prompt Agent + MongoDB 共享上下文 + 复用 RAG 检索**"。不需要任何新中间件。

---

### 11.2 前置改造：增强 `GlmApiService`（阶段0，必须先做）

现有 `chat(String prompt)` 有三个致命限制：系统提示写死、`max_tokens=500` 太短、异常被吞进返回字符串（Agent 无法感知失败）。**新增一个可控重载，保留旧方法不动**：

```java
// 在 GlmApiService 中新增（不改动原 chat 方法，避免影响现有 AgentController）
/**
 * 面向 Agent 的可控调用：自定义系统提示、温度、最大 token；失败抛异常（由调用方决定降级）。
 */
public String chat(String systemPrompt, String userPrompt,
                   double temperature, int maxTokens) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", "Bearer " + apiKey);

    Map<String, Object> body = new HashMap<>();
    body.put("model", model);
    body.put("temperature", temperature);
    body.put("max_tokens", maxTokens);
    body.put("messages", List.of(
        Map.of("role", "system", "content", systemPrompt),
        Map.of("role", "user",   "content", userPrompt)
    ));

    ResponseEntity<String> resp = restTemplate.exchange(
        apiUrl, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    if (resp.getStatusCode() != HttpStatus.OK) {
        throw new RuntimeException("GLM API 非 200：" + resp.getStatusCode());
    }
    JsonNode json = objectMapper.readTree(resp.getBody());   // 注意：readTree 抛 checked 异常，用 try/catch 包一层或方法 throws
    JsonNode content = json.path("choices").path(0).path("message").path("content");
    if (content.isMissingNode()) throw new RuntimeException("GLM 响应无 content：" + resp.getBody());
    return content.asText();
}
```

**验收**：`./mvnw -q -DskipTests compile` 通过；写一个临时单测或 `main` 调用它返回非空（需设置环境变量 `GLM_API_KEY`）。

---

### 11.3 数据模型（阶段0，完整字段，直接建类）

放在 `com.edu.backend.agent.model`。用 Lombok `@Data`。**这些是内存 DTO，不是 MongoDB 实体**（持久化实体见 §11.4）。

```java
public enum AgentType { PLANNER, TUTOR, ASSESSOR, QA, RECOMMENDER, MONITOR }

public enum AgentStatus { SUCCESS, FAILED, SKIPPED, TIMEOUT }

@Data
public class Task {                 // 用户发起的顶层协作任务
    private String taskId;          // UUID
    private String userId;          // 关联 users._id
    private String goal;            // 用户目标，如"帮我规划第3章电磁场的学习并出一套测验"
    private String subject;         // 学科/课程，可空
    private Map<String,Object> params = new HashMap<>();  // 额外参数
}

@Data
public class SubTask {              // 分解后分配给单个 Agent 的子任务
    private String subTaskId;       // UUID
    private String parentTaskId;
    private AgentType assignedAgent;
    private int priority;           // 数值越大越先执行
    private List<String> dependsOn = new ArrayList<>(); // 依赖的 subTaskId（DAG 边）
    private Map<String,Object> input = new HashMap<>(); // 该 Agent 的输入
}

@Data
public class AgentResult {          // 单个 Agent 的产出
    private AgentType agentType;
    private AgentStatus status;
    private Object data;            // 结构化产出（各 Agent 不同，见 §11.5）
    private String rawText;         // GLM 原始返回，便于追溯
    private String error;           // status!=SUCCESS 时的原因
    private long elapsedMs;
}

@Data
public class CollaborationResult {  // 编排器汇总
    private String taskId;
    private AgentStatus overallStatus;
    private Map<AgentType, AgentResult> perAgent = new LinkedHashMap<>();
    private String summary;         // 面向用户的综合结论
    private List<Map<String,Object>> citations = new ArrayList<>(); // 复用 RAG 溯源
}

@Data
public class AgentContext {         // 传给每个 Agent 的共享上下文（只读快照）
    private String userId;
    private String taskId;
    private Map<String,Object> userProfile = new HashMap<>(); // 来自 users/user_progress/user_grades
    private List<AgentResult> upstreamResults = new ArrayList<>(); // 依赖的上游 Agent 结果
    private String retrievedKnowledge;  // 来自 RAGKnowledgeBaseService 的检索片段
}
```

---

### 11.4 持久化实体与集合（阶段0）

放在 `com.edu.backend.agent.persistence`。三张新集合，全部 `agent_` 前缀：

| 集合 | 用途 | 关键字段 |
|---|---|---|
| `agent_runs` | 每次协作任务的完整记录（追溯/前端历史） | `_id, userId, goal, overallStatus, perAgentJson, summary, createTime, elapsedMs` |
| `agent_messages` | Agent 间/编排过程的消息流水（可视化协作过程） | `_id, taskId, sender, receiver, type, contentJson, timestamp` |
| `agent_context` | 用户级共享状态（替代 Redis） | `_id=userId, profileJson, lastTaskId, updateTime` |

```java
@Data @Document("agent_runs")
public class AgentRun {
    @Id private String id;
    @Indexed private String userId;
    private String goal;
    private String overallStatus;
    private String perAgentJson;   // CollaborationResult.perAgent 序列化
    private String summary;
    private Long elapsedMs;
    private Date createTime;
}
// AgentMessageDoc / AgentContextDoc 同理，用 MongoTemplate 或 Repository 存取
```

**验收**：启动后端，调用一次写入，`mongosh` 里 `db.agent_runs.find()` 能看到记录。

---

### 11.5 核心范式：每个 Agent = 一次"结构化 GLM 调用"（阶段1）

这是**整个系统的心脏**。所有 Agent 继承同一基类，差异只在 **system prompt / user prompt 模板 / 输出 JSON 结构**。

```java
public abstract class AbstractGlmAgent {

    @Autowired protected GlmApiService glm;
    @Autowired protected ObjectMapper objectMapper;

    public abstract AgentType getType();
    protected abstract String systemPrompt();                 // 角色设定，固定
    protected abstract String buildUserPrompt(SubTask t, AgentContext ctx); // 拼输入
    protected abstract double temperature();                  // 各 Agent 不同
    protected abstract int maxTokens();

    /** 模板方法：统一的"调用→解析→降级→计时"流程 */
    public AgentResult run(SubTask subTask, AgentContext ctx) {
        long start = System.currentTimeMillis();
        AgentResult r = new AgentResult();
        r.setAgentType(getType());
        try {
            String user = buildUserPrompt(subTask, ctx);
            String raw = glm.chat(systemPrompt(), user, temperature(), maxTokens());
            r.setRawText(raw);
            r.setData(parse(raw));           // 解析为结构化数据
            r.setStatus(AgentStatus.SUCCESS);
        } catch (Exception e) {
            r.setStatus(AgentStatus.FAILED);
            r.setError(e.getMessage());
            r.setData(fallback(subTask, ctx)); // 降级产出，保证编排不中断
        }
        r.setElapsedMs(System.currentTimeMillis() - start);
        return r;
    }

    /** 健壮 JSON 解析：容忍 ```json 包裹、前后多余文字 */
    protected JsonNode parse(String raw) throws Exception {
        String s = raw.trim();
        int i = s.indexOf('{'), j = s.lastIndexOf('}');
        if (i >= 0 && j > i) s = s.substring(i, j + 1);
        return objectMapper.readTree(s);
    }

    protected Object fallback(SubTask t, AgentContext ctx) { return Map.of("degraded", true); }
}
```

**给所有 prompt 的通用尾巴（强制 JSON）**：每个 `buildUserPrompt` 结尾都追加：
```
请严格只输出 JSON，不要输出任何解释文字、不要用 markdown 代码块包裹。JSON 结构如下：
{ ...本 Agent 的输出 schema... }
```

**验收（阶段1 里程碑）**：先只实现 `TutorAgent`，用一个真实问题跑通 `run()`，返回结构化 JSON。这是"单 Agent 打通 GLM"的最小闭环。

---

### 11.6 六个 Agent 的 Prompt 规格（阶段2–3）

下表给出每个 Agent 的 system prompt 要点、输入、输出 JSON schema。DeepSeek 按此实现各子类的三个抽象方法。

| Agent | system prompt 角色 | 输入(来自 ctx / subTask.input) | 输出 JSON schema |
|---|---|---|---|
| **TutorAgent** | "资深学科教师，擅长把知识点讲清楚" | 知识主题、学情画像、RAG 检索片段 | `{"title","sections":[{"heading","content"}],"keyPoints":[],"difficulty":"易/中/难"}` |
| **PlannerAgent** | "学习路径规划专家" | 学习目标、当前掌握度 | `{"steps":[{"order","topic","objective","estMinutes"}],"totalMinutes"}` |
| **AssessorAgent** | "命题与测评专家" | 主题、难度、题量 | `{"questions":[{"type","stem","options?","answer","explanation","score"}],"totalScore"}` |
| **QaAgent** | "耐心答疑助教" | 用户问题、RAG 检索片段 | `{"answer","references":[],"followUp":[]}`（应复用 `RAGKnowledgeBaseService.enhancedQA`，把 citations 带出） |
| **RecommenderAgent** | "学习资源推荐官" | 用户画像、薄弱点 | `{"recommendations":[{"resource","reason","priority"}]}` |
| **MonitorAgent** | "学习行为分析与预警" | 近期学习/成绩数据 | `{"risks":[{"type","level","evidence"}],"suggestions":[]}` |

示例（TutorAgent，DeepSeek 照此写其余五个）：
```java
@Component
public class TutorAgent extends AbstractGlmAgent {
    public AgentType getType() { return AgentType.TUTOR; }
    protected double temperature() { return 0.5; }
    protected int maxTokens() { return 1500; }
    protected String systemPrompt() {
        return "你是一名资深学科教师，擅长将知识点讲解得条理清晰、由浅入深。只依据提供的资料作答，资料不足时明确说明。";
    }
    protected String buildUserPrompt(SubTask t, AgentContext ctx) {
        String topic = String.valueOf(t.getInput().getOrDefault("topic", ctx.getTaskId()));
        return """
            请围绕主题【%s】生成教学内容。
            学情参考：%s
            可用资料：%s
            请严格只输出 JSON，不要 markdown 包裹，结构：
            {"title":"","sections":[{"heading":"","content":""}],"keyPoints":[""],"difficulty":"中"}
            """.formatted(topic, ctx.getUserProfile(), ctx.getRetrievedKnowledge());
    }
}
```

---

### 11.7 编排器、REST 与前端（阶段3–4）

**编排器**（现实版，替代蓝图的 `CollaborationManager` + 线程/MQ）：

```java
@Service
public class CollaborationOrchestrator {
    private final Map<AgentType, AbstractGlmAgent> registry;  // Spring 注入所有 Agent 后按 getType() 建索引
    private final TaskDecomposer decomposer;
    private final SharedContextService contextService;
    private final ResultAggregator aggregator;
    private final ExecutorService pool = Executors.newFixedThreadPool(4); // 有界线程池

    public CollaborationResult execute(Task task) {
        List<SubTask> subs = decomposer.decompose(task);          // 规则分解：见 §11.1，根据 goal 关键词决定启用哪些 Agent
        AgentContext base = contextService.loadContext(task);     // 拉 user_profile + RAG 检索
        Map<AgentType, AgentResult> done = new LinkedHashMap<>();

        // 按依赖分层：无依赖的并行，有依赖的等上游（拓扑分批）
        for (List<SubTask> layer : topoLayers(subs)) {
            List<Future<AgentResult>> fs = layer.stream().map(st -> pool.submit(() -> {
                AgentContext ctx = contextService.withUpstream(base, done, st); // 注入上游结果
                return registry.get(st.getAssignedAgent()).run(st, ctx);
            })).toList();
            for (int i = 0; i < fs.size(); i++) {
                try { AgentResult r = fs.get(i).get(30, TimeUnit.SECONDS); done.put(r.getAgentType(), r); }
                catch (Exception e) { /* 记 TIMEOUT/FAILED，继续 */ }
            }
        }
        CollaborationResult result = aggregator.aggregate(task, done);
        contextService.persistRun(task, result);                  // 落库 agent_runs
        return result;
    }
}
```

**REST 契约**（新增 `AgentCollaborationController`，不动旧 `/api/agent/chat`）：
```
POST /api/agent/collaborate
Req : { "userId":"...", "goal":"帮我规划第3章的学习并生成一套测验", "subject":"电磁场" }
Resp: { "success":true, "data": CollaborationResult }   // 复用现有 BaseResponse/统一响应风格
```

**前端**：复用 `qianduan/src/components/AgentChat.vue` 交互；新增一个"协作结果卡片"组件，分区展示 `perAgent` 里各 Agent 的产出（规划步骤 / 教学内容 / 测验题 / 推荐 / 预警），并渲染 `citations`。API 客户端加 `collaborate(payload)` 方法到 `src/api/agent.ts`。

---

### 11.8 共享上下文服务（阶段4，替代 Redis/Neo4j）

```java
@Service
public class SharedContextService {
    @Autowired private MongoTemplate mongo;
    @Autowired private RAGKnowledgeBaseService rag;   // 复用现有 RAG
    private final Map<String, AgentContext> cache = new ConcurrentHashMap<>(); // 进程内短期缓存

    public AgentContext loadContext(Task task) {
        AgentContext ctx = new AgentContext();
        ctx.setUserId(task.getUserId());
        ctx.setTaskId(task.getTaskId());
        ctx.setUserProfile(loadUserProfile(task.getUserId()));            // 查 users/user_progress/user_grades
        Map<String,Object> ragResult = rag.enhancedQA(task.getGoal(), ""); // 混合检索
        ctx.setRetrievedKnowledge(String.valueOf(ragResult.get("answer")));
        return ctx;
    }
    // withUpstream(base, done, subTask)：复制 base 并塞入该子任务依赖的上游 AgentResult
}
```

---

### 11.9 分阶段落地路线（每阶段可独立编译/演示，附给 DeepSeek 的提示词）

> 每个阶段结束都必须编译通过并能演示，**严格按顺序**，不要跳步。

| 阶段 | 目标（做完能演示什么） | 新建/改动 | 验收标准 |
|---|---|---|---|
| **0 地基** | 项目能编译，GLM 可控调用 + 数据模型 + 空集合就绪 | §11.2 增强 `GlmApiService`；§11.3 模型；§11.4 实体+Repository | `mvnw compile` 通过；能写一条 `agent_runs` |
| **1 单 Agent** | `TutorAgent` 单独跑通，返回结构化教学 JSON | `AbstractGlmAgent` + `TutorAgent` + 一个临时测试端点 | 输入主题→返回合法 JSON（含 title/sections） |
| **2 串行编排** | 规划→教学→评估 三 Agent 串行协作 | `PlannerAgent/AssessorAgent` + `TaskDecomposer`(规则) + `CollaborationOrchestrator`(先串行) + `AgentCollaborationController` | `POST /collaborate` 返回三段产出 |
| **3 并行+聚合** | 六 Agent 齐全，按 DAG 分层并行，结果聚合+落库 | 其余三 Agent + 拓扑分层 + `ResultAggregator` + 有界线程池 | 单次请求 6 Agent 均有产出；`agent_runs` 有完整记录；超时不整体失败 |
| **4 上下文+前端** | 共享上下文注入 + 前端可视化协作结果 | `SharedContextService`（复用 RAG/用户数据） + 前端协作卡片 | 前端一屏展示规划/教学/测验/推荐/预警 + 溯源 |
| **5 策略优化（可选/研究增量）** | 协作策略自适应 | 先 `HeuristicStrategy`（加权评分选 Agent 组合）；论文需要再上轻量 bandit | 有 A/B 对比数据即可，不阻塞前四阶段交付 |

**可复制给 DeepSeek 的阶段提示词模板**（以阶段2为例）：
```
在 Spring Boot 项目 houduan/backend 内，包 com.edu.backend.agent 下实现"多 Agent 串行协作"阶段2。
已具备：GlmApiService.chat(system,user,temp,maxTokens)、AbstractGlmAgent 基类、TutorAgent、model 与 persistence。
本步要求：
1. 实现 PlannerAgent、AssessorAgent（继承 AbstractGlmAgent，prompt/schema 见方案 §11.6）。
2. 实现 TaskDecomposer.decompose(Task)：按 goal 关键词决定启用哪些 Agent，建立 规划→教学→评估 的 dependsOn 依赖。
3. 实现 CollaborationOrchestrator.execute(Task)：先按依赖串行调用，注入上游结果到 AgentContext。
4. 新增 AgentCollaborationController：POST /api/agent/collaborate，请求/响应契约见 §11.7，复用现有统一响应封装。
硬约束：不引入任何新依赖；不改动已有 AgentController；每个方法完整实现无 TODO；提交前 ./mvnw -q -DskipTests compile 必须通过。
输出：改动的文件清单 + 每个文件的完整代码 + 一条 curl 验证命令。
```

---

### 11.10 测试与验收（贯穿各阶段）

- **单元测试**：用 Mockito mock `GlmApiService`，给定固定 JSON 返回，断言各 Agent 的 `parse()` 与降级逻辑正确（不消耗真实 API 配额）。
- **黄金样例**：每个 Agent 准备 1–2 组"输入→期望结构"样例，作为回归基线。
- **集成测试**：`@SpringBootTest` 起真实容器 + 内嵌/本地 Mongo，跑一次 `/collaborate`，断言六 Agent 均有产出且 `agent_runs` 落库。
- **健壮性**：注入 GLM 超时/返回非 JSON/空内容三种故障，验证编排不整体崩溃、降级产出可用。

### 11.11 指导 DeepSeek 的反模式清单（Code Review 必查）

1. ❌ 擅自 `pom.xml` 加 Redis/Neo4j/Kafka/Python 桥接 → ✅ 一律用 §11.1 的现有实现替代。
2. ❌ 用正则从 GLM 自由文本抠字段 → ✅ 强制 JSON + `parse()` 健壮解析。
3. ❌ `Executors.newCachedThreadPool()` 或裸 `new Thread` 无上限 → ✅ 有界 `newFixedThreadPool(4)`，防上传/并发打爆。
4. ❌ 把 GLM 调用异常吞掉返回假数据当成功 → ✅ `status=FAILED` + 明确 `error` + 降级数据，编排继续。
5. ❌ 直接把用户输入拼进 system prompt（提示注入） → ✅ 用户内容只进 user 消息，且对超长内容截断。
6. ❌ 改动 `controller/AgentController.java` 现有端点 → ✅ 新增 `AgentCollaborationController` 隔离。
7. ❌ 一次性把六个 Agent + 编排 + 前端全写完再编译 → ✅ 严格按 §11.9 分阶段，每阶段编译通过再推进。
8. ❌ 在 GLM 请求里发送真实密钥到日志/前端 → ✅ Key 只从环境变量 `GLM_API_KEY` 读取（本项目已改造）。

---

*本文档为研究生毕业设计研究方向一：多Agent协作系统的详细技术方案*
