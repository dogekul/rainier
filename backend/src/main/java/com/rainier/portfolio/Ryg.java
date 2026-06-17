/* (C) 2026 Rainier — internal use only. */
package com.rainier.portfolio;

/**
 * Deterministic project red/yellow/green health (v0.0.28). Mirrors the frontend {@code ryg.ts} rule
 * so the server-side portfolio and the client panels agree: GRAY when no open work; RED if anything is
 * blocked or the overdue ratio exceeds the threshold; YELLOW if some (but few) overdue; GREEN otherwise.
 */
public final class Ryg {

  /** A project goes RED above this overdue ratio (of its open tasks). */
  public static final double OVERDUE_RED_THRESHOLD = 0.3;

  private Ryg() {}

  public static String tier(int openCount, int overdueCount, int blockedCount) {
    if (openCount == 0) {
      return "GRAY";
    }
    if (blockedCount > 0) {
      return "RED";
    }
    double ratio = (double) overdueCount / openCount;
    if (ratio > OVERDUE_RED_THRESHOLD) {
      return "RED";
    }
    if (overdueCount > 0) {
      return "YELLOW";
    }
    return "GREEN";
  }

  /** Sort order red &lt; yellow &lt; green &lt; gray (worst first). */
  public static int order(String tier) {
    switch (tier) {
      case "RED":
        return 0;
      case "YELLOW":
        return 1;
      case "GREEN":
        return 2;
      default:
        return 3;
    }
  }
}
