package com.edu.backend.agent;

import java.util.Map;

public class AgentMessage {
    private String messageId;
    private AgentType sender;
    private AgentType receiver;
    private MessageType type;
    private Object content;
    private Map<String, Object> metadata;
    private long timestamp;

    public AgentMessage() {
        this.timestamp = System.currentTimeMillis();
    }

    public static AgentMessage createRequest(AgentType sender, AgentType receiver, Object content) {
        AgentMessage msg = new AgentMessage();
        msg.messageId = java.util.UUID.randomUUID().toString();
        msg.sender = sender;
        msg.receiver = receiver;
        msg.type = MessageType.REQUEST;
        msg.content = content;
        return msg;
    }

    public static AgentMessage createResponse(AgentType sender, AgentType receiver, Object content) {
        AgentMessage msg = new AgentMessage();
        msg.messageId = java.util.UUID.randomUUID().toString();
        msg.sender = sender;
        msg.receiver = receiver;
        msg.type = MessageType.RESPONSE;
        msg.content = content;
        return msg;
    }

    public static AgentMessage createError(AgentType sender, AgentType receiver, String error) {
        AgentMessage msg = new AgentMessage();
        msg.messageId = java.util.UUID.randomUUID().toString();
        msg.sender = sender;
        msg.receiver = receiver;
        msg.type = MessageType.ERROR;
        msg.content = error;
        return msg;
    }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public AgentType getSender() { return sender; }
    public void setSender(AgentType sender) { this.sender = sender; }
    public AgentType getReceiver() { return receiver; }
    public void setReceiver(AgentType receiver) { this.receiver = receiver; }
    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }
    public Object getContent() { return content; }
    public void setContent(Object content) { this.content = content; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
