/* (C) 2026 Rainier — internal use only. */
package com.rainier.link.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Entity types a link can attach to (v0.0.31). Constant-class enum (Java 8, no Set.of). */
public final class LinkTargetType {

  public static final String STORY = "STORY";
  public static final String TASK = "TASK";

  public static final Set<String> ALL =
      Collections.unmodifiableSet(new HashSet<>(Arrays.asList(STORY, TASK)));

  private LinkTargetType() {}
}
