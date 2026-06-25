/* (C) 2026 Rainier — internal use only. */
package com.rainier.ai;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 默认 {@link AiInference} 实现（v0.0.68, A4）—— 纯模板 deterministic stub。后续接入真实 LLM 时
 * 用新实现替换。
 *
 * <p>规则：
 *
 * <ul>
 *   <li>{@code String.class} → {@code "stub:" + taskKind}
 *   <li>{@code Integer.class} / {@code int.class} → {@code 0}
 *   <li>{@code Long.class} / {@code long.class} → {@code 0L}
 *   <li>{@code Boolean.class} / {@code boolean.class} → {@code Boolean.FALSE}
 *   <li>其它类型 → 尝试无参构造一个对象；不行则 throw {@link IllegalArgumentException}
 * </ul>
 */
@Component
@Primary
public class StubAiInference implements AiInference {

  @Override
  @SuppressWarnings("unchecked")
  public <T> T infer(String taskKind, Object input, Class<T> outputClass) {
    if (outputClass == null) {
      throw new IllegalArgumentException("outputClass must not be null");
    }
    String kind = taskKind == null ? "UNKNOWN" : taskKind;
    if (outputClass.equals(String.class)) {
      return (T) ("stub:" + kind);
    }
    if (outputClass.equals(Integer.class) || outputClass.equals(int.class)) {
      return (T) Integer.valueOf(0);
    }
    if (outputClass.equals(Long.class) || outputClass.equals(long.class)) {
      return (T) Long.valueOf(0L);
    }
    if (outputClass.equals(Boolean.class) || outputClass.equals(boolean.class)) {
      return (T) Boolean.FALSE;
    }
    try {
      return outputClass.getDeclaredConstructor().newInstance();
    } catch (ReflectiveOperationException e) {
      throw new IllegalArgumentException(
          "StubAiInference cannot produce a stub for " + outputClass.getName(), e);
    }
  }
}
