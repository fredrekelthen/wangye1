package com.edu.backend.llm;

import java.util.List;

/**
 * 大模型调用能力的 <b>Service Definition</b>（能力 seam 的定义方）。
 *
 * <p>对齐生产级 harness 的「capability seam」三角色模型：
 * <ul>
 *   <li><b>Service Definition</b>：本接口——声明能力，不关心谁实现。</li>
 *   <li><b>Service Provider</b>：{@code GlmLlmProvider}——实现具体厂商协议。</li>
 *   <li><b>Consumer</b>：{@code GlmApiService} 及各 Agent——只依赖本接口。</li>
 * </ul>
 * 三者分离后，替换厂商（GLM → DeepSeek → 本地 vLLM）只需新增一个实现类，
 * 消费方零改动。
 *
 * <p><b>约定（两侧都要遵守）</b>：
 * 实现必须把内部所有失败形式（抛异常、非 200、协议成功但缺字段）
 * 统一规范化为 {@link LlmException}；成功则必须返回非 {@code null} 的
 * {@link LlmCallResult}。绝不返回"看起来像答案的错误文本"。
 */
public interface LlmProvider {

    /** 提供方标识，用于日志、追踪与按路由选择。 */
    String id();

    /**
     * 执行一次模型调用。
     *
     * @param messages 完整消息序列（含 system），顺序即模型所见顺序
     * @param options  温度、最大 token、超时
     * @return 调用结果，永不为 {@code null}
     * @throws LlmException 任何失败，已规范化
     */
    LlmCallResult chat(List<LlmMessage> messages, LlmCallOptions options);
}
