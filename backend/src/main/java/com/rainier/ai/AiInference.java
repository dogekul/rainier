/* (C) 2026 Rainier — internal use only. */
package com.rainier.ai;

/**
 * 飞轮所有 AI 调用的统一契约（v0.0.68, A4）。后续接入真实 LLM / 模型时只需新增一个实现并标记
 * {@code @Primary}，业务代码无需更改。
 *
 * <p>本版默认绑定 {@link StubAiInference}（deterministic stub，便于测试与开发）。
 */
public interface AiInference {

  /**
   * 根据 {@code taskKind} 与输入 {@code input}，推理出一个 {@code outputClass} 类型的结果。
   *
   * @param taskKind 推理任务类型，例如 {@code "SYNC_TASK_STATUS"} / {@code "RISK_RADAR"}。
   * @param input 推理输入（任意对象，子实现自行解释）。
   * @param outputClass 期望的输出类型。
   * @return 推理结果（非空）。
   */
  <T> T infer(String taskKind, Object input, Class<T> outputClass);
}
