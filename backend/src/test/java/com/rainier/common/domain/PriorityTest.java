/* (C) 2026 Rainier — internal use only. */
package com.rainier.common.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * TC-PRIO-001 — the shared {@link Priority} now has 5 levels incl. LOWEST. Demand / Requirement /
 * Story / Task all validate against {@code Priority.ALL}, so this proves LOWEST is accepted across
 * all four entities.
 */
class PriorityTest {

  @Test
  void all_hasFiveLevelsIncludingLowest() {
    assertEquals(5, Priority.ALL.size());
    assertTrue(Priority.ALL.contains(Priority.LOWEST));
    assertTrue(
        Priority.ALL.containsAll(
            Arrays.asList(
                Priority.URGENT, Priority.HIGH, Priority.MEDIUM, Priority.LOW, Priority.LOWEST)));
    assertEquals("LOWEST", Priority.LOWEST);
  }
}
