package com.edu.backend.agent;

import com.edu.backend.service.RAGKnowledgeBaseService;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class SharedKnowledgeBase {

    private final MongoTemplate mongoTemplate;
    private final RAGKnowledgeBaseService ragService;

    private final Map<String, Map<String, Object>> sessionStates = new HashMap<>();
    private final Map<String, List<Map<String, Object>>> conversationHistory = new HashMap<>();
    private final Map<String, Map<String, Object>> agentSharedMemory = new HashMap<>();

    public SharedKnowledgeBase(MongoTemplate mongoTemplate, RAGKnowledgeBaseService ragService) {
        this.mongoTemplate = mongoTemplate;
        this.ragService = ragService;
    }

    public void saveSessionState(String sessionId, Map<String, Object> state) {
        sessionStates.put(sessionId, state);
    }

    public Map<String, Object> getSessionState(String sessionId) {
        return sessionStates.getOrDefault(sessionId, new HashMap<>());
    }

    public void addConversationEntry(String sessionId, String role, String content) {
        conversationHistory.computeIfAbsent(sessionId, k -> new ArrayList<>())
            .add(Map.of("role", role, "content", content, "timestamp", LocalDateTime.now().toString()));
    }

    public List<Map<String, Object>> getConversationHistory(String sessionId) {
        return conversationHistory.getOrDefault(sessionId, new ArrayList<>());
    }

    public List<Map<String, Object>> getRecentConversations(String sessionId, int limit) {
        List<Map<String, Object>> history = getConversationHistory(sessionId);
        int from = Math.max(0, history.size() - limit);
        return history.subList(from, history.size());
    }

    public void putAgentMemory(String key, Map<String, Object> value) {
        agentSharedMemory.put(key, value);
    }

    public Map<String, Object> getAgentMemory(String key) {
        return agentSharedMemory.get(key);
    }

    public Map<String, Object> getAllAgentMemory() {
        return new HashMap<>(agentSharedMemory);
    }

    public void clearSession(String sessionId) {
        sessionStates.remove(sessionId);
        conversationHistory.remove(sessionId);
    }

    public Map<String, Object> getKnowledgeStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("activeSessions", sessionStates.size());
        stats.put("totalConversations", conversationHistory.values().stream().mapToInt(List::size).sum());
        stats.put("sharedMemoryKeys", agentSharedMemory.size());

        Map<String, Object> ragStats = ragService.getKnowledgeBaseStats();
        stats.putAll(ragStats);
        return stats;
    }
}
