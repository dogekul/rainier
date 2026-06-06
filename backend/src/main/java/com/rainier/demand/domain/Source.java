/* (C) 2026 Rainier — internal use only. */
package com.rainier.demand.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Source channel for {@link Demand} submission. */
public final class Source {
  public static final String WEB = "WEB";
  public static final String WECHAT = "WECHAT";
  public static final String EMAIL = "EMAIL";
  public static final String DINGTALK = "DINGTALK";
  public static final String OTHER = "OTHER";

  public static final Set<String> ALL =
      Collections.unmodifiableSet(
          new HashSet<>(Arrays.asList(WEB, WECHAT, EMAIL, DINGTALK, OTHER)));

  private Source() {}
}
