package com.edu.backend.agent;

import java.util.List;

public interface BaseAgent {

    AgentResult processTask(Task task);

    void updateState(AgentState state);

    AgentType getAgentType();

    List<String> getCapabilities();

    AgentState getCurrentState();
}
