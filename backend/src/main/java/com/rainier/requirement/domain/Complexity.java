/* (C) 2026 Rainier — internal use only. */
package com.rainier.requirement.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Complexity sizing for {@link Requirement}. Nullable in DB; null = unscored. */
public final class Complexity {
  public static final String XS = "XS";
  public static final String S = "S";
  public static final String M = "M";
  public static final String L = "L";
  public static final String XL = "XL";

  public static final Set<String> ALL =
      Collections.unmodifiableSet(new HashSet<>(Arrays.asList(XS, S, M, L, XL)));

  private Complexity() {}
}
