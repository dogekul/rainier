/* (C) 2026 Rainier — internal use only. */
package com.rainier.portfolio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** v0.0.28 — deterministic RYG rule (mirror of frontend ryg.ts). Covers TC-RYG-B01..05. */
class RygTest {

  @Test
  void noOpenWork_isGray() {
    assertEquals("GRAY", Ryg.tier(0, 0, 0));
  }

  @Test
  void anyBlocked_isRed() {
    assertEquals("RED", Ryg.tier(10, 0, 1));
  }

  @Test
  void overdueRatioAboveThreshold_isRed() {
    assertEquals("RED", Ryg.tier(10, 4, 0)); // 0.4 > 0.3
    assertEquals("YELLOW", Ryg.tier(10, 3, 0)); // 0.3 not > 0.3
  }

  @Test
  void someOverdue_yellow_noneOverdue_green() {
    assertEquals("YELLOW", Ryg.tier(10, 1, 0));
    assertEquals("GREEN", Ryg.tier(10, 0, 0));
  }

  @Test
  void order_redBeforeYellowBeforeGreenBeforeGray() {
    assertTrue(Ryg.order("RED") < Ryg.order("YELLOW"));
    assertTrue(Ryg.order("YELLOW") < Ryg.order("GREEN"));
    assertTrue(Ryg.order("GREEN") < Ryg.order("GRAY"));
  }
}
