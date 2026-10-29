package com.edu.backend.agent;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AgentRegistry {

    private final Map<AgentType, BaseAgent> agents = new ConcurrentHashMap<>();
    private final Map<AgentType, List<BaseAgent>> agentPool = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        for (AgentType type : AgentType.values()) {
            agentPool.put(type, new ArrayList<>());
        }
    }

    public void register(AgentType type, BaseAgent agent) {
        agents.put(type, agent);
        agentPool.computeIfAbsent(type, k -> new ArrayList<>()).add(agent);
    }

    public BaseAgent getAgent(AgentType type) {
        return agents.get(type);
    }

    public List<BaseAgent> getAgents(AgentType type) {
        return agentPool.getOrDefault(type, Collections.emptyList());
    }

    public Map<AgentType, BaseAgent> getAllAgents() {
        return new HashMap<>(agents);
    }

    public Set<AgentType> getRegisteredTypes() {
        return agents.keySet();
    }

    public boolean hasAgent(AgentType type) {
        return agents.containsKey(type);
    }
}
