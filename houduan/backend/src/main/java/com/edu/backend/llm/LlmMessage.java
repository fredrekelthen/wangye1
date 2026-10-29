package com.edu.backend.llm;

/**
 * 一条模型可见的消息。
 *
 * <p>不可变值对象：构造后不再修改，便于在重试时安全复用同一个列表。
 */
public final class LlmMessage {

    public enum Role {
        SYSTEM("system"),
        USER("user"),
        ASSISTANT("assistant");

        private final String wireName;

        Role(String wireName) {
            this.wireName = wireName;
        }

        public String getWireName() {
            return wireName;
        }
    }

    private final Role role;
    private final String content;

    private LlmMessage(Role role, String content) {
        if (role == null) {
            throw new IllegalArgumentException("role 不能为空");
        }
        this.role = role;
        this.content = content == null ? "" : content;
    }

    public static LlmMessage system(String content) {
        return new LlmMessage(Role.SYSTEM, content);
    }

    public static LlmMessage user(String content) {
        return new LlmMessage(Role.USER, content);
    }

    public static LlmMessage assistant(String content) {
        return new LlmMessage(Role.ASSISTANT, content);
    }

    public Role getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        return role.getWireName() + ": " + content;
    }
}
