package com.edu.backend.llm;

import java.time.Duration;

/**
 * 单次模型调用的不可变选项。
 */
public final class LlmCallOptions {

    /**
     * 默认 max_tokens。
     *
     * <p><b>为什么不是 800</b>：当前模型 {@code deepseek-flash} 是<b>推理模型</b>，
     * 思考过程（reasoning）与正式回答（content）<b>共享</b>同一份 max_tokens 额度。
     * 额度偏小时，复杂任务会把额度全部用于思考，导致 {@code finish_reason=length}
     * 且 content 为空字符串——表现为"调用成功但答案为空"。
     * 实测简单问答思考约 250 tokens，而"生成整章学习内容"这类任务思考可达数千。</p>
     *
     * <p>提高上限<b>不会增加成本</b>：max_tokens 只是上限，按实际生成量计费。</p>
     */
    private static final int DEFAULT_MAX_TOKENS = 4000;

    /** 默认超时：额度提高后单次生成更久，故从 60s 放宽到 120s。 */
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(120);

    private static final LlmCallOptions DEFAULTS =
            new LlmCallOptions(0.7d, DEFAULT_MAX_TOKENS, DEFAULT_TIMEOUT);

    private final double temperature;
    private final int maxTokens;
    private final Duration timeout;

    private LlmCallOptions(double temperature, int maxTokens, Duration timeout) {
        if (maxTokens <= 0) {
            throw new IllegalArgumentException("maxTokens 必须为正数");
        }
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout 必须为正数");
        }
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.timeout = timeout;
    }

    public static LlmCallOptions defaults() {
        return DEFAULTS;
    }

    public LlmCallOptions withTemperature(double temperature) {
        return new LlmCallOptions(temperature, maxTokens, timeout);
    }

    public LlmCallOptions withMaxTokens(int maxTokens) {
        return new LlmCallOptions(temperature, maxTokens, timeout);
    }

    public LlmCallOptions withTimeout(Duration timeout) {
        return new LlmCallOptions(temperature, maxTokens, timeout);
    }

    public double getTemperature() {
        return temperature;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public Duration getTimeout() {
        return timeout;
    }
}
