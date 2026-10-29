package com.edu.backend.agent;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

/**
 * 多智能体协作管理器（优化版）
 *
 * 相对旧版的主要改进：
 *  1. 共享线程池：不再每次 executeTask 新建/销毁线程池，改为全局守护线程池。
 *  2. 依赖感知的并行：按 SubTask 依赖关系做拓扑分层，同层并行、跨层串行，
 *     使 TaskDecomposer 中定义的依赖（教学依赖规划、评估依赖教学）真正生效。
 *  3. 失败重试：单个 Agent 超时/异常时自动重试一次，避免单点抖动拖垮整体。
 *  4. 通信与记忆接入：下游 Agent 执行前注入上游产出（共享上下文），
 *     执行后写入 SharedKnowledgeBase 并通过 AgentMessageQueue 向下游投递消息。
 *  5. 状态机与追踪：贯穿使用 WAITING / COLLABORATING / ERROR 状态，
 *     并记录完整协作链路便于观测。
 */
@Slf4j
@Service
public class CollaborationManager {

    private final AgentRegistry agentRegistry;
    private final TaskDecomposer decomposer;
    private final TaskAllocator allocator;
    private final ResultAggregator aggregator;
    private final SharedKnowledgeBase sharedKnowledgeBase;
    private final AgentMessageQueue messageQueue;

    private final ExecutorService executor;

    public CollaborationManager(AgentRegistry agentRegistry, TaskDecomposer decomposer,
                                TaskAllocator allocator, ResultAggregator aggregator,
                                SharedKnowledgeBase sharedKnowledgeBase, AgentMessageQueue messageQueue) {
        this.agentRegistry = agentRegistry;
        this.decomposer = decomposer;
        this.allocator = allocator;
        this.aggregator = aggregator;
        this.sharedKnowledgeBase = sharedKnowledgeBase;
        this.messageQueue = messageQueue;
        int poolSize = Math.max(4, Runtime.getRuntime().availableProcessors());
        this.executor = Executors.newFixedThreadPool(poolSize, r -> {
            Thread t = new Thread(r, "agent-collab-");
            t.setDaemon(true);
            return t;
        });
    }

    @PreDestroy
    public void destroy() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public CollaborationResult executeTask(Task task) {
        List<String> trace = new ArrayList<>();
        List<AgentResult> finalResults = new ArrayList<>();

        List<SubTask> subTasks = decomposer.decomposeTask(task);
        List<TaskAllocator.AllocationResult> allocations = allocator.allocateTasks(subTasks);

        List<ExecItem> items = new ArrayList<>();
        Map<String, SubTask> byId = new HashMap<>();
        Map<String, AgentType> idToType = new HashMap<>();
        for (TaskAllocator.AllocationResult ar : allocations) {
            if (ar.isSuccess() && ar.getAssignedAgent() != null) {
                SubTask st = ar.getTask();
                ExecItem item = new ExecItem(st, ar.getAssignedAgent(), st.getAssignedAgent());
                items.add(item);
                byId.put(st.getId(), st);
                idToType.put(st.getId(), st.getAssignedAgent());
            } else {
                AgentType missing = ar.getTask() != null ? ar.getTask().getAssignedAgent() : null;
                trace.add("⚠ 分配失败：" + missing);
                if (missing != null) {
                    AgentResult fr = AgentResult.failure("未找到可用的 " + missing + " 智能体");
                    fr.setMetadata(new LinkedHashMap<>());
                    fr.getMetadata().put("agentType", missing.name());
                    finalResults.add(fr);
                }
            }
        }

        // 反向依赖：记录每个 Agent 的下游依赖方，用于执行后消息通知
        Map<AgentType, List<AgentType>> dependents = new HashMap<>();
        for (ExecItem it : items) {
            for (String depId : it.subTask.getDependencies()) {
                AgentType depType = idToType.get(depId);
                if (depType != null) {
                    dependents.computeIfAbsent(depType, k -> new ArrayList<>()).add(it.type);
                }
            }
        }

        Map<AgentType, AgentResult> intermediateResults = new ConcurrentHashMap<>();
        List<List<ExecItem>> levels = computeLevels(items, byId);

        for (int li = 0; li < levels.size(); li++) {
            List<ExecItem> level = levels.get(li);
            String phaseInfo = level.stream().map(e -> e.type.name())
                    .reduce((a, b) -> a + ", " + b).orElse("");
            trace.add("▶ 第 " + (li + 1) + " 阶段并行执行：" + phaseInfo);
            if (li > 0) {
                for (ExecItem e : level) e.agent.updateState(AgentState.WAITING);
            }

            List<Callable<ExecOutcome>> callables = new ArrayList<>();
            for (ExecItem e : level) {
                final ExecItem item = e;
                callables.add(() -> {
                    item.agent.updateState(AgentState.COLLABORATING);
                    Task agentTask = buildAgentTask(task, item, byId, intermediateResults);
                    long start = System.currentTimeMillis();
                    AgentResult result = item.agent.processTask(agentTask);
                    result.setExecutionTimeMs(System.currentTimeMillis() - start);
                    if (result.getMetadata() == null) result.setMetadata(new LinkedHashMap<>());
                    result.getMetadata().put("agentType", item.type.name());
                    return new ExecOutcome(item, result);
                });
            }

            List<Future<ExecOutcome>> futures = new ArrayList<>();
            for (Callable<ExecOutcome> c : callables) futures.add(executor.submit(c));

            for (int i = 0; i < futures.size(); i++) {
                ExecOutcome outcome = runWithRetry(futures.get(i), callables.get(i), level.get(i));
                intermediateResults.put(outcome.item.type, outcome.result);
                finalResults.add(outcome.result);
                recordOutcome(task, outcome, trace, dependents);
            }
        }

        // 协作链路追踪写入共享知识库，便于观测与复盘
        Map<String, Object> traceMap = new LinkedHashMap<>();
        traceMap.put("steps", trace);
        traceMap.put("totalAgents", items.size());
        sharedKnowledgeBase.putAgentMemory(task.getId() + ":trace", traceMap);
        log.info("[Collaboration] 任务 " + task.getId() + " 协作链路：\n" + String.join("\n", trace));

        return aggregator.aggregate(finalResults, task);
    }

    /** 拓扑分层：依赖未满足的子任务进入下一阶段，同阶段内并行执行 */
    private List<List<ExecItem>> computeLevels(List<ExecItem> items, Map<String, SubTask> byId) {
        List<List<ExecItem>> levels = new ArrayList<>();
        Set<String> remaining = new LinkedHashSet<>();
        items.forEach(it -> remaining.add(it.subTask.getId()));
        Set<String> completed = new HashSet<>();

        while (!remaining.isEmpty()) {
            List<ExecItem> level = new ArrayList<>();
            for (String id : remaining) {
                SubTask st = byId.get(id);
                if (depsSatisfied(st, completed, byId)) {
                    items.stream().filter(it -> it.subTask.getId().equals(id))
                            .findFirst().ifPresent(level::add);
                }
            }
            if (level.isEmpty()) { // 检测到环，强制放行剩余节点避免死锁
                for (String id : remaining) {
                    items.stream().filter(it -> it.subTask.getId().equals(id))
                            .findFirst().ifPresent(level::add);
                }
            }
            levels.add(level);
            for (ExecItem it : level) {
                remaining.remove(it.subTask.getId());
                completed.add(it.subTask.getId());
            }
        }
        return levels;
    }

    private boolean depsSatisfied(SubTask st, Set<String> completed, Map<String, SubTask> byId) {
        if (st.getDependencies() == null) return true;
        for (String depId : st.getDependencies()) {
            if (depId == null || depId.isEmpty()) continue;
            if (!byId.containsKey(depId)) continue; // 外部依赖视为已满足
            if (!completed.contains(depId)) return false;
        }
        return true;
    }

    /** 构造下游 Agent 的子任务，并注入上游 Agent 的产出作为共享上下文 */
    private Task buildAgentTask(Task parent, ExecItem item, Map<String, SubTask> byId,
                                Map<AgentType, AgentResult> intermediate) {
        Task t = new Task();
        t.setId(parent.getId() + "-" + item.type.name());
        t.setContent(parent.getContent());
        t.setDomain(parent.getDomain());
        t.setUserId(parent.getUserId());
        t.setPriority(parent.getPriority());
        t.setParams(parent.getParams());

        Map<String, Object> ctx = new LinkedHashMap<>();
        for (String depId : item.subTask.getDependencies()) {
            if (depId == null || depId.isEmpty()) continue;
            SubTask dep = byId.get(depId);
            if (dep == null) continue;
            AgentResult upstream = intermediate.get(dep.getAssignedAgent());
            if (upstream != null && upstream.isSuccess() && upstream.getData() != null) {
                ctx.put(dep.getAssignedAgent().name(), upstream.getData());
            }
        }
        t.setContext(ctx);
        return t;
    }

    /** 执行并失败自动重试一次，避免单点抖动导致整任务残缺 */
    private ExecOutcome runWithRetry(Future<ExecOutcome> future, Callable<ExecOutcome> callable, ExecItem item) {
        try {
            return future.get(60, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            try {
                return executor.submit(callable).get(60, TimeUnit.SECONDS);
            } catch (Exception e2) {
                return failed(item, item.type + " 执行超时（含一次重试）");
            }
        } catch (Exception e) {
            try {
                return executor.submit(callable).get(60, TimeUnit.SECONDS);
            } catch (Exception e2) {
                return failed(item, item.type + " 执行异常（含一次重试）：" + e.getMessage());
            }
        }
    }

    private ExecOutcome failed(ExecItem item, String msg) {
        AgentResult r = AgentResult.failure(msg);
        r.setMetadata(new LinkedHashMap<>());
        r.getMetadata().put("agentType", item.type.name());
        return new ExecOutcome(item, r);
    }

    private void recordOutcome(Task task, ExecOutcome outcome, List<String> trace,
                               Map<AgentType, List<AgentType>> dependents) {
        AgentType type = outcome.item.type;
        AgentResult result = outcome.result;
        if (result.isSuccess()) {
            Map<String, Object> mem = new LinkedHashMap<>();
            mem.put("success", true);
            mem.put("data", result.getData());
            sharedKnowledgeBase.putAgentMemory(type.name(), mem);
            // 通过消息队列向下游 Agent 投递产出，激活 AgentMessageQueue 通信链路
            for (AgentType dep : dependents.getOrDefault(type, Collections.emptyList())) {
                messageQueue.sendMessage(AgentMessage.createResponse(type, dep, result.getData()));
            }
            sharedKnowledgeBase.addConversationEntry(task.getId(), type.name(),
                    "✓ " + type.name() + " 完成，耗时 " + result.getExecutionTimeMs() + "ms");
            trace.add("✓ " + type.name() + " 成功（" + result.getExecutionTimeMs() + "ms）");
        } else {
            outcome.item.agent.updateState(AgentState.ERROR);
            sharedKnowledgeBase.addConversationEntry(task.getId(), type.name(),
                    "✗ " + type.name() + " 失败：" + result.getErrorMessage());
            trace.add("✗ " + type.name() + " 失败：" + result.getErrorMessage());
        }
    }

    public Map<String, Object> getSystemStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("totalAgents", agentRegistry.getRegisteredTypes().size());
        status.put("agentTypes", agentRegistry.getRegisteredTypes());

        Map<String, String> states = new LinkedHashMap<>();
        for (AgentType type : agentRegistry.getRegisteredTypes()) {
            BaseAgent agent = agentRegistry.getAgent(type);
            states.put(type.name(), agent.getCurrentState().name());
        }
        status.put("agentStates", states);
        return status;
    }

    private static final class ExecItem {
        final SubTask subTask;
        final BaseAgent agent;
        final AgentType type;
        ExecItem(SubTask subTask, BaseAgent agent, AgentType type) {
            this.subTask = subTask; this.agent = agent; this.type = type;
        }
    }

    private static final class ExecOutcome {
        final ExecItem item;
        final AgentResult result;
        ExecOutcome(ExecItem item, AgentResult result) { this.item = item; this.result = result; }
    }
}
