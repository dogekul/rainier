/* (C) 2026 Rainier — internal use only. */
package com.rainier.ai.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** AI 错误公示板的状态常量（v0.0.68）。 */
public final class AiErrorStatus {

  public static final String OPEN = "OPEN";
  public static final String FIXED = "FIXED";

  public static final Set<String> ALL =
      Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(OPEN, FIXED)));

  private AiErrorStatus() {}
}
