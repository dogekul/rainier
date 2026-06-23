/* (C) 2026 Rainier — internal use only. */
package com.rainier.opportunity.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Decision recorded at a pipeline gate (v0.0.44): {@code PASS} advances, {@code REJECT} loses the deal. */
public final class GateDecision {
  public static final String PASS = "PASS";
  public static final String REJECT = "REJECT";

  public static final Set<String> ALL =
      Collections.unmodifiableSet(new HashSet<>(Arrays.asList(PASS, REJECT)));

  private GateDecision() {}
}
