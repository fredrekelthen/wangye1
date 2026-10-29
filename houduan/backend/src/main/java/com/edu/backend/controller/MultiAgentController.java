package com.edu.backend.controller;

import com.edu.backend.agent.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/multi-agent")
public class MultiAgentController {

    private final CollaborationManager collaborationManager;
    private final AgentRegistry agentRegistry;
    private final AgentMessageQueue messageQueue;
    private final SharedKnowledgeBase knowledgeBase;
    private final ProgrammingIntentDetector intentDetector;

    public MultiAgentController(CollaborationManager collaborationManager,
                                AgentRegistry agentRegistry,
                                AgentMessageQueue messageQueue,
                                SharedKnowledgeBase knowledgeBase,
                                ProgrammingIntentDetector intentDetector) {
        this.collaborationManager = collaborationManager;
        this.agentRegistry = agentRegistry;
        this.messageQueue = messageQueue;
        this.knowledgeBase = knowledgeBase;
        this.intentDetector = intentDetector;
    }

    @PostMapping("/task")
    public ResponseEntity<Map<String, Object>> submitTask(@RequestBody Map<String, Object> request) {
        try {
            String content = (String) request.get("content");
            String domain = (String) request.getOrDefault("domain", "通用");
            String userId = (String) request.getOrDefault("userId", "anonymous");
            String priorityStr = (String) request.getOrDefault("priority", "NORMAL");

            Task task = new Task();
            task.setContent(content);
            task.setDomain(domain);
            task.setUserId(userId);
            task.setPriority(Task.TaskPriority.valueOf(priorityStr.toUpperCase()));
            task.setParams(request);
            task.setStatus(Task.TaskStatus.IN_PROGRESS);

            CollaborationResult result = collaborationManager.executeTask(task);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("taskId", task.getId());
            response.put("status", "completed");
            response.put("result", result);
            response.put("decomposedAgents", agentRegistry.getRegisteredTypes());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", e.getMessage());
            error.put("success", false);
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/qa")
    public ResponseEntity<Map<String, Object>> askQuestion(@RequestBody Map<String, Object> request) {
        try {
            String question = (String) request.get("question");
            String userId = (String) request.getOrDefault("userId", "anonymous");

            Task task = new Task();
            task.setContent(question);
            task.setUserId(userId);
            task.setDomain("问答");

            BaseAgent qaAgent = agentRegistry.getAgent(AgentType.QA);
            if (qaAgent == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "答疑Agent未注册"));
            }

            AgentResult result = qaAgent.processTask(task);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", result.isSuccess());
            response.put("data", result.getData());
            response.put("executionTimeMs", result.getExecutionTimeMs());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/teach")
    public ResponseEntity<Map<String, Object>> generateTeaching(@RequestBody Map<String, Object> request) {
        try {
            String topic = (String) request.get("topic");
            String domain = (String) request.getOrDefault("domain", "通用");

            Task task = new Task();
            task.setContent(topic);
            task.setDomain(domain);

            BaseAgent tutorAgent = agentRegistry.getAgent(AgentType.TUTOR);
            if (tutorAgent == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "教学Agent未注册"));
            }

            AgentResult result = tutorAgent.processTask(task);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", result.isSuccess());
            response.put("data", result.getData());
            response.put("executionTimeMs", result.getExecutionTimeMs());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/assess")
    public ResponseEntity<Map<String, Object>> generateAssessment(@RequestBody Map<String, Object> request) {
        try {
            String topic = (String) request.get("topic");
            String domain = (String) request.getOrDefault("domain", "通用");

            Task task = new Task();
            task.setContent(topic);
            task.setDomain(domain);
            task.setParams(request);

            BaseAgent assessorAgent = agentRegistry.getAgent(AgentType.ASSESSOR);
            if (assessorAgent == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "评估Agent未注册"));
            }

            AgentResult result = assessorAgent.processTask(task);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", result.isSuccess());
            response.put("data", result.getData());
            response.put("executionTimeMs", result.getExecutionTimeMs());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/assess/submit")
    public ResponseEntity<Map<String, Object>> submitAssessment(@RequestBody Map<String, Object> request) {
        try {
            Task task = new Task();
            task.setParams(request);

            BaseAgent assessorAgent = agentRegistry.getAgent(AgentType.ASSESSOR);
            if (assessorAgent == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "评估Agent未注册"));
            }

            AgentResult result = assessorAgent.processTask(task);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", result.isSuccess());
            response.put("data", result.getData());
            response.put("executionTimeMs", result.getExecutionTimeMs());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/plan")
    public ResponseEntity<Map<String, Object>> generatePlan(@RequestBody Map<String, Object> request) {
        try {
            String goal = (String) request.get("goal");

            Task task = new Task();
            task.setContent(goal);

            BaseAgent plannerAgent = agentRegistry.getAgent(AgentType.PLANNER);
            if (plannerAgent == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "规划Agent未注册"));
            }

            AgentResult result = plannerAgent.processTask(task);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", result.isSuccess());
            response.put("data", result.getData());
            response.put("executionTimeMs", result.getExecutionTimeMs());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        Map<String, Object> status = collaborationManager.getSystemStatus();

        Map<String, Object> registered = new LinkedHashMap<>();
        for (AgentType type : agentRegistry.getRegisteredTypes()) {
            BaseAgent agent = agentRegistry.getAgent(type);
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("state", agent.getCurrentState().name());
            info.put("capabilities", agent.getCapabilities());
            registered.put(type.name(), info);
        }
        status.put("agents", registered);
        status.put("knowledgeBaseStats", knowledgeBase.getKnowledgeStats());

        return ResponseEntity.ok(status);
    }

    @PostMapping("/message/send")
    public ResponseEntity<Map<String, Object>> sendMessage(@RequestBody Map<String, Object> request) {
        try {
            String senderStr = (String) request.get("sender");
            String receiverStr = (String) request.get("receiver");
            String content = (String) request.get("content");

            AgentType sender = AgentType.valueOf(senderStr.toUpperCase());
            AgentType receiver = AgentType.valueOf(receiverStr.toUpperCase());

            AgentMessage message = AgentMessage.createRequest(sender, receiver, content);
            messageQueue.sendMessage(message);

            return ResponseEntity.ok(Map.of("success", true, "messageId", message.getMessageId()));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 直达入口：依据自然语言需求生成电磁场仿真 Python 代码。
     * 该入口直接调用 EM_CODE 智能体；领域闸门在智能体内部完成（非电磁领域直接拒识）。
     */
    @PostMapping("/em-code")
    public ResponseEntity<Map<String, Object>> generateEMCode(@RequestBody Map<String, Object> request) {
        try {
            String requirement = (String) request.get("requirement");
            String userId = (String) request.getOrDefault("userId", "anonymous");

            Task task = new Task();
            task.setContent(requirement);
            task.setDomain("电磁场仿真");
            task.setUserId(userId);
            task.setParams(request);

            BaseAgent codeAgent = agentRegistry.getAgent(AgentType.EM_CODE);
            if (codeAgent == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "电磁仿真代码智能体未注册"));
            }

            AgentResult result = codeAgent.processTask(task);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", result.isSuccess());
            response.put("data", result.getData());
            response.put("executionTimeMs", result.getExecutionTimeMs());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 意图检测：判断文本是否具备编程意图、是否属于电磁场领域，以及是否应激活代码智能体。
     */
    @PostMapping("/intent/detect")
    public ResponseEntity<Map<String, Object>> detectIntent(@RequestBody Map<String, Object> request) {
        String text = (String) request.get("text");
        ProgrammingIntentDetector.IntentDecision decision = intentDetector.detect(text);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("programmingIntent", decision.isProgrammingIntent());
        response.put("emDomain", decision.isEmDomain());
        response.put("activateCodeAgent", decision.isActivateCodeAgent());
        return ResponseEntity.ok(response);
    }
}
