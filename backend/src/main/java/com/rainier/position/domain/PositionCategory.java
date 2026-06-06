/* (C) 2026 Rainier — internal use only. */
package com.rainier.position.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Category bucket for {@link Position}. Stored as VARCHAR(16); validated at service layer. */
public final class PositionCategory {
  public static final String TECH = "TECH";
  public static final String BIZ = "BIZ";
  public static final String PM = "PM";
  public static final String MGMT = "MGMT";
  public static final String OTHER = "OTHER";

  public static final Set<String> ALL =
      Collections.unmodifiableSet(new HashSet<>(Arrays.asList(TECH, BIZ, PM, MGMT, OTHER)));

  private PositionCategory() {}
}
