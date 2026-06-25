/* (C) 2026 Rainier — internal use only. */
package com.rainier.milestone.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rainier.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link MilestoneStatusMachine} — v0.0.87 (C7). */
class MilestoneStatusMachineTest {

  /** Allowed: PLANNED → IN_PROGRESS. */
  @Test
  void planned_to_inProgress_ok() {
    assertDoesNotThrow(
        () ->
            MilestoneStatusMachine.validateTransition(
                MilestoneStatus.PLANNED, MilestoneStatus.IN_PROGRESS));
  }

  /** Allowed: PLANNED → CANCELLED. */
  @Test
  void planned_to_cancelled_ok() {
    assertDoesNotThrow(
        () ->
            MilestoneStatusMachine.validateTransition(
                MilestoneStatus.PLANNED, MilestoneStatus.CANCELLED));
  }

  /** Allowed: IN_PROGRESS → DONE. */
  @Test
  void inProgress_to_done_ok() {
    assertDoesNotThrow(
        () ->
            MilestoneStatusMachine.validateTransition(
                MilestoneStatus.IN_PROGRESS, MilestoneStatus.DONE));
  }

  /** Allowed: IN_PROGRESS → CANCELLED. */
  @Test
  void inProgress_to_cancelled_ok() {
    assertDoesNotThrow(
        () ->
            MilestoneStatusMachine.validateTransition(
                MilestoneStatus.IN_PROGRESS, MilestoneStatus.CANCELLED));
  }

  /** Allowed: DONE → IN_PROGRESS (撤销). */
  @Test
  void done_to_inProgress_ok() {
    assertDoesNotThrow(
        () ->
            MilestoneStatusMachine.validateTransition(
                MilestoneStatus.DONE, MilestoneStatus.IN_PROGRESS));
  }

  /** Allowed: CANCELLED → PLANNED (撤销). */
  @Test
  void cancelled_to_planned_ok() {
    assertDoesNotThrow(
        () ->
            MilestoneStatusMachine.validateTransition(
                MilestoneStatus.CANCELLED, MilestoneStatus.PLANNED));
  }

  /** Same → same is always ok (no-op). */
  @Test
  void same_to_same_ok() {
    assertDoesNotThrow(
        () ->
            MilestoneStatusMachine.validateTransition(
                MilestoneStatus.DONE, MilestoneStatus.DONE));
    assertDoesNotThrow(
        () ->
            MilestoneStatusMachine.validateTransition(
                MilestoneStatus.PLANNED, MilestoneStatus.PLANNED));
  }

  /** Forbidden: PLANNED → DONE (must go via IN_PROGRESS). */
  @Test
  void planned_to_done_forbidden() {
    BadRequestException ex =
        assertThrows(
            BadRequestException.class,
            () ->
                MilestoneStatusMachine.validateTransition(
                    MilestoneStatus.PLANNED, MilestoneStatus.DONE));
    assertTrue(ex.getMessage().contains("illegal transition"));
    assertTrue(ex.getMessage().contains("PLANNED"));
    assertTrue(ex.getMessage().contains("DONE"));
  }

  /** Forbidden: DONE → CANCELLED. */
  @Test
  void done_to_cancelled_forbidden() {
    assertThrows(
        BadRequestException.class,
        () ->
            MilestoneStatusMachine.validateTransition(
                MilestoneStatus.DONE, MilestoneStatus.CANCELLED));
  }

  /** Forbidden: DONE → PLANNED (must go via IN_PROGRESS). */
  @Test
  void done_to_planned_forbidden() {
    assertThrows(
        BadRequestException.class,
        () ->
            MilestoneStatusMachine.validateTransition(
                MilestoneStatus.DONE, MilestoneStatus.PLANNED));
  }

  /** Forbidden: CANCELLED → DONE. */
  @Test
  void cancelled_to_done_forbidden() {
    assertThrows(
        BadRequestException.class,
        () ->
            MilestoneStatusMachine.validateTransition(
                MilestoneStatus.CANCELLED, MilestoneStatus.DONE));
  }

  /** Forbidden: CANCELLED → IN_PROGRESS (must go via PLANNED). */
  @Test
  void cancelled_to_inProgress_forbidden() {
    assertThrows(
        BadRequestException.class,
        () ->
            MilestoneStatusMachine.validateTransition(
                MilestoneStatus.CANCELLED, MilestoneStatus.IN_PROGRESS));
  }

  /** Null sides → BadRequest. */
  @Test
  void null_sides_throw() {
    assertThrows(
        BadRequestException.class,
        () -> MilestoneStatusMachine.validateTransition(null, MilestoneStatus.PLANNED));
    assertThrows(
        BadRequestException.class,
        () -> MilestoneStatusMachine.validateTransition(MilestoneStatus.PLANNED, null));
  }

  /** Legacy aliases must be normalized via {@link MilestoneStatus#normalize(String)}. */
  @Test
  void normalize_legacyAliases() {
    org.junit.jupiter.api.Assertions.assertEquals(
        MilestoneStatus.DONE, MilestoneStatus.normalize(MilestoneStatus.REACHED));
    org.junit.jupiter.api.Assertions.assertEquals(
        MilestoneStatus.CANCELLED, MilestoneStatus.normalize(MilestoneStatus.MISSED));
    org.junit.jupiter.api.Assertions.assertEquals(
        MilestoneStatus.PLANNED, MilestoneStatus.normalize(MilestoneStatus.PLANNED));
    org.junit.jupiter.api.Assertions.assertEquals("XXX", MilestoneStatus.normalize("XXX"));
    org.junit.jupiter.api.Assertions.assertNull(MilestoneStatus.normalize(null));
  }
}
