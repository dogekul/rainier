/* (C) 2026 Rainier — internal use only. */
package com.rainier.opportunity.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Deal outcome of an {@link Opportunity} (v0.0.44), separate from its stage. {@code OPEN} = in
 * progress; {@code WON} = 赢单 (合同签订 done); {@code LOST} = 丢单 (a pre-sales gate rejected). Powers
 * the 成单率/中标率 metric (WON / (WON+LOST)).
 */
public final class OpportunityStatus {
  public static final String OPEN = "OPEN";
  public static final String WON = "WON";
  public static final String LOST = "LOST";

  public static final Set<String> ALL =
      Collections.unmodifiableSet(new HashSet<>(Arrays.asList(OPEN, WON, LOST)));

  private OpportunityStatus() {}
}
