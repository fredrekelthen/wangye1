package com.edu.backend.agent;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TaskDecomposer {

    private final ProgrammingIntentDetector intentDetector;

    public TaskDecomposer(ProgrammingIntentDetector intentDetector) {
        this.intentDetector = intentDetector;
    }

    public List<SubTask> decomposeTask(Task task) {
        TaskAnalysis analysis = analyzeTask(task);
        List<AgentType> requiredAgents = determineRequiredAgents(analysis, task);
        List<SubTask> subTasks = generateSubTasks(task, requiredAgents);
        buildTaskDependencies(subTasks);
        return optimizeTaskAllocation(subTasks);
    }

    public TaskAnalysis analyzeTask(Task task) {
        TaskAnalysis analysis = new TaskAnalysis();
        analysis.setComplexity(evaluateComplexity(task));
        analysis.setDomain(task.getDomain());
        analysis.setHasAssessment(hasAssessmentKeywords(task.getContent()));
        analysis.setHasQuestions(hasQuestionKeywords(task.getContent()));
        analysis.setNeedsResources(needsResources(task.getContent()));
        analysis.setRequiresPlanning(true);
        analysis.setNeedsMonitoring(task.getPriority() == Task.TaskPriority.URGENT);
        return analysis;
    }

    private int evaluateComplexity(Task task) {
        String content = task.getContent() != null ? task.getContent() : "";
        int score = 1;
        if (content.length() > 100) score++;
        if (content.contains("规划") || content.contains("计划")) score++;
        if (content.contains("考试") || content.contains("测试") || content.contains("评估")) score++;
        if (content.contains("复习") || content.contains("总结")) score++;
        return Math.min(score, 5);
    }

    private boolean hasAssessmentKeywords(String content) {
        if (content == null) return false;
        return containsAny(content, "考试", "测试", "评估", "检查", "作业", "测验", "评分");
    }

    private boolean hasQuestionKeywords(String content) {
        if (content == null) return false;
        return containsAny(content, "问题", "什么是", "为什么", "怎么做", "如何", "请教", "不明白", "不懂");
    }

    private boolean needsResources(String content) {
        if (content == null) return false;
        return containsAny(content, "推荐", "资料", "资源", "材料", "参考", "拓展");
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    private List<AgentType> determineRequiredAgents(TaskAnalysis analysis, Task task) {
        List<AgentType> agents = new ArrayList<>();
        agents.add(AgentType.TUTOR);
        if (analysis.isRequiresPlanning()) agents.add(AgentType.PLANNER);
        if (analysis.isHasAssessment()) agents.add(AgentType.ASSESSOR);
        if (analysis.isHasQuestions()) agents.add(AgentType.QA);
        if (analysis.isNeedsResources()) agents.add(AgentType.RECOMMENDER);
        if (analysis.isNeedsMonitoring()) agents.add(AgentType.MONITOR);
        // 检测到明确的编程意图时，自动激活电磁场仿真代码生成智能体
        if (intentDetector.hasProgrammingIntent(task.getContent())) {
            agents.add(AgentType.EM_CODE);
        }
        return agents;
    }

    private List<SubTask> generateSubTasks(Task task, List<AgentType> agents) {
        List<SubTask> subTasks = new ArrayList<>();
        for (AgentType agent : agents) {
            SubTask subTask = new SubTask();
            subTask.setParentTaskId(task.getId());
            subTask.setAssignedAgent(agent);
            subTask.setDescription("任务[" + task.getId().substring(0, 8) + "]的" + agent + "子任务");
            subTask.setPriority(task.getPriority());
            subTask.setInputSpec(Map.of(
                "taskContent", task.getContent() != null ? task.getContent() : "",
                "domain", task.getDomain() != null ? task.getDomain() : "",
                "userId", task.getUserId() != null ? task.getUserId() : ""
            ));
            subTask.setOutputSpec(Map.of("agentType", agent.name()));
            subTasks.add(subTask);
        }
        return subTasks;
    }

    private void buildTaskDependencies(List<SubTask> subTasks) {
        for (SubTask st : subTasks) {
            if (st.getAssignedAgent() == AgentType.TUTOR) {
                st.getDependencies().add(filterByAgent(subTasks, AgentType.PLANNER).stream()
                    .map(SubTask::getId).findFirst().orElse(""));
            }
            if (st.getAssignedAgent() == AgentType.ASSESSOR) {
                st.getDependencies().add(filterByAgent(subTasks, AgentType.TUTOR).stream()
                    .map(SubTask::getId).findFirst().orElse(""));
            }
            st.getDependencies().removeIf(String::isEmpty);
        }
    }

    private List<SubTask> filterByAgent(List<SubTask> tasks, AgentType type) {
        return tasks.stream()
            .filter(t -> t.getAssignedAgent() == type)
            .toList();
    }

    private List<SubTask> optimizeTaskAllocation(List<SubTask> subTasks) {
        subTasks.sort(Comparator.comparing(s -> mapPriority(s.getPriority())));
        for (SubTask st : subTasks) {
            if (st.getAssignedAgent() == AgentType.MONITOR) {
                st.setPriority(Task.TaskPriority.LOW);
            }
        }
        return subTasks;
    }

    private int mapPriority(Task.TaskPriority p) {
        if (p == null) return 1;
        return switch (p) {
            case URGENT -> 0;
            case HIGH -> 1;
            case NORMAL -> 2;
            case LOW -> 3;
        };
    }

    public static class TaskAnalysis {
        private int complexity;
        private String domain;
        private boolean hasAssessment;
        private boolean hasQuestions;
        private boolean needsResources;
        private boolean requiresPlanning;
        private boolean needsMonitoring;

        public int getComplexity() { return complexity; }
        public void setComplexity(int complexity) { this.complexity = complexity; }
        public String getDomain() { return domain; }
        public void setDomain(String domain) { this.domain = domain; }
        public boolean isHasAssessment() { return hasAssessment; }
        public void setHasAssessment(boolean hasAssessment) { this.hasAssessment = hasAssessment; }
        public boolean isHasQuestions() { return hasQuestions; }
        public void setHasQuestions(boolean hasQuestions) { this.hasQuestions = hasQuestions; }
        public boolean isNeedsResources() { return needsResources; }
        public void setNeedsResources(boolean needsResources) { this.needsResources = needsResources; }
        public boolean isRequiresPlanning() { return requiresPlanning; }
        public void setRequiresPlanning(boolean requiresPlanning) { this.requiresPlanning = requiresPlanning; }
        public boolean isNeedsMonitoring() { return needsMonitoring; }
        public void setNeedsMonitoring(boolean needsMonitoring) { this.needsMonitoring = needsMonitoring; }
    }
}
