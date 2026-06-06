/* (C) 2026 Rainier — internal use only. */
package com.rainier.demandrequirement.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Link type between a Demand and a Requirement. */
public final class LinkType {
  public static final String DERIVED = "DERIVED";
  public static final String RELATED = "RELATED";

  public static final Set<String> ALL =
      Collections.unmodifiableSet(new HashSet<>(Arrays.asList(DERIVED, RELATED)));

  private LinkType() {}
}
