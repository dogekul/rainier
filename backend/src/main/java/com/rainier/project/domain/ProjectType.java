/* (C) 2026 Rainier — internal use only. */
package com.rainier.project.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Type values for {@link Project} — extensible enum-as-constants, same pattern as {@link
 * ProjectStatus}.
 *
 * <p>v0.0.16 seeds the 轻量/正式 (casual/formal) distinction from role-card §2.3. The field is named
 * generically ({@code projectType}) and stored as VARCHAR so future values can be appended without a
 * DB enum migration. "Conversion" 轻量→正式 is a plain {@code update} of this value — no approval, no
 * completeness gate (A2 narrowed). Only enum-membership is validated.
 */
public final class ProjectType {
  public static final String CASUAL = "CASUAL";
  public static final String FORMAL = "FORMAL";

  public static final Set<String> ALL =
      Collections.unmodifiableSet(new HashSet<>(Arrays.asList(CASUAL, FORMAL)));

  private ProjectType() {}
}
