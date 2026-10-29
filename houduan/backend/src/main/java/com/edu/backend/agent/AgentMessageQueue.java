package com.edu.backend.agent;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Component
public class AgentMessageQueue {

    private final Map<AgentType, BlockingQueue<AgentMessage>> queues = new ConcurrentHashMap<>();

    public AgentMessageQueue() {
        for (AgentType type : AgentType.values()) {
            queues.put(type, new LinkedBlockingQueue<>(100));
        }
    }

    public void sendMessage(AgentMessage message) {
        if (message == null) return;
        BlockingQueue<AgentMessage> queue = queues.get(message.getReceiver());
        if (queue != null) {
            queue.offer(message);
        }
    }

    public AgentMessage receiveMessage(AgentType agentType, long timeoutMs) {
        BlockingQueue<AgentMessage> queue = queues.get(agentType);
        if (queue == null) return null;
        try {
            return queue.poll(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    public int getQueueSize(AgentType agentType) {
        BlockingQueue<AgentMessage> queue = queues.get(agentType);
        return queue != null ? queue.size() : 0;
    }

    public void clearQueue(AgentType agentType) {
        BlockingQueue<AgentMessage> queue = queues.get(agentType);
        if (queue != null) queue.clear();
    }
}
