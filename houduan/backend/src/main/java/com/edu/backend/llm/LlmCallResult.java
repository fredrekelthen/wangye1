package com.edu.backend.llm;

/**
 * 一次成功模型调用的结果。
 *
 * <p><b>正交事实独立上报</b>：内容、用量、结束原因各自独立暴露，不做互相推断。
 * 特别地，{@code finishReason == "length"} 表示输出被 max_tokens 截断——
 * 这是一次<em>成功</em>的调用，但内容是不完整的。调用方若把它当成完整答案，
 * 就会把半句话持久化进会话历史。因此该信息必须可被读取。
 *
 * <p>用量字段为 {@code null} 表示服务端未回传，不要当成 0。
 */
public final class LlmCallResult {

    private final String content;
    private final String model;
    private final Integer promptTokens;
    private final Integer completionTokens;
    private final Integer totalTokens;
    private final String finishReason;

    public LlmCallResult(String content, String model, Integer promptTokens,
                         Integer completionTokens, Integer totalTokens, String finishReason) {
        this.content = content == null ? "" : content;
        this.model = model;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
        this.finishReason = finishReason;
    }

    public String getContent() {
        return content;
    }

    public String getModel() {
        return model;
    }

    public Integer getPromptTokens() {
        return promptTokens;
    }

    public Integer getCompletionTokens() {
        return completionTokens;
    }

    public Integer getTotalTokens() {
        return totalTokens;
    }

    /** 结束原因；{@code null} 表示服务端未回传。 */
    public String getFinishReason() {
        return finishReason;
    }

    /** 输出是否被 max_tokens 截断（内容不完整）。与"调用是否成功"正交。 */
    public boolean isTruncated() {
        return "length".equalsIgnoreCase(finishReason);
    }
}
