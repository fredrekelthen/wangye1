package com.edu.backend.llm;

/**
 * 大模型调用的结构化失败。
 *
 * <p>设计依据（对齐生产级 agent harness 的两条防御性规则）：
 * <ul>
 *   <li><b>正交结果独立上报</b>：一次失败的调用可以同时具有多种性质（例如"超时"且"HTTP 200"）。
 *       因此 {@link #getKind()}、{@link #getHttpStatus()}、{@link #isRetryable()} 各自独立暴露，
 *       调用方不得用其中一个去推断另一个。</li>
 *   <li><b>公共约定两侧都要遵守</b>：本异常是 LLM 边界上<em>唯一</em>的失败表示。
 *       适配器内部无论是抛异常、返回非 200，还是返回"协议成功但内容缺失"的响应，
 *       在这里一律被规范化为 {@code LlmException}。消费方因此不必猜测失败来源。</li>
 * </ul>
 *
 * <p><b>反模式（本项目曾存在）</b>：把错误文本当正常回复返回
 * （{@code return "与 GLM API 通信时发生错误: " + e.getMessage();}）。
 * 该字符串会被上层当作 AI 答案<em>持久化进数据库</em>，污染会话历史且不可恢复。
 * 失败必须是失败，降级必须由调用方显式声明。
 */
public class LlmException extends RuntimeException {

    public enum Kind {
        /** 401/403：API Key 缺失、无效或无权限。重试无意义。 */
        AUTH,
        /** 429：限流。可重试，且应退避。 */
        RATE_LIMIT,
        /** 请求体超过模型上下文窗口。重试无意义，需先压缩上下文。 */
        CONTEXT_OVERFLOW,
        /** 连接或读取超时。可重试。 */
        TIMEOUT,
        /** 网络层失败（DNS、连接重置、SSL）。可重试。 */
        TRANSPORT,
        /** 5xx：服务端错误。可重试。 */
        SERVER,
        /** 协议层成功（HTTP 200）但响应缺失必要字段或无法解析。重试无意义。 */
        BAD_RESPONSE,
        /** 未归类。保守地视为可重试。 */
        UNKNOWN
    }

    private final Kind kind;
    private final Integer httpStatus;

    public LlmException(Kind kind, String message) {
        this(kind, message, null, null);
    }

    public LlmException(Kind kind, String message, Integer httpStatus, Throwable cause) {
        super(message, cause);
        this.kind = kind == null ? Kind.UNKNOWN : kind;
        this.httpStatus = httpStatus;
    }

    public Kind getKind() {
        return kind;
    }

    /** HTTP 状态码；网络层失败时为 {@code null}。与 {@link #getKind()} 正交。 */
    public Integer getHttpStatus() {
        return httpStatus;
    }

    /** 本次失败是否值得重试。与 {@link #getKind()} 正交（不要用一个推另一个）。 */
    public boolean isRetryable() {
        return switch (kind) {
            case RATE_LIMIT, TIMEOUT, TRANSPORT, SERVER, UNKNOWN -> true;
            case AUTH, CONTEXT_OVERFLOW, BAD_RESPONSE -> false;
        };
    }
}
