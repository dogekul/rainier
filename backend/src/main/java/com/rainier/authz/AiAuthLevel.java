/* (C) 2026 Rainier — internal use only. */
package com.rainier.authz;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * v0.0.69 (A5): AI 授权级别。BASIC = 仅建议；INTERMEDIATE = 可改非敏感字段（status/desc）；DEPTH =
 * 可改任何未被 {@link FieldLock} 锁定的字段。本版仅定义常量；实际拦截在后续 A6/A7 写路径加。
 */
public final class AiAuthLevel {

  public static final String BASIC = "BASIC";
  public static final String INTERMEDIATE = "INTERMEDIATE";
  public static final String DEPTH = "DEPTH";

  public static final Set<String> ALL =
      Collections.unmodifiableSet(new HashSet<>(Arrays.asList(BASIC, INTERMEDIATE, DEPTH)));

  private AiAuthLevel() {}
}
