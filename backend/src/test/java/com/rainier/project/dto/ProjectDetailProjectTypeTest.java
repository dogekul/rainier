/* (C) 2026 Rainier — internal use only. */
package com.rainier.project.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.rainier.project.domain.Project;
import org.junit.jupiter.api.Test;

/** TC-PROJTYPE-010 — read-path null→CASUAL coalesce in {@link ProjectDetail#from}. */
class ProjectDetailProjectTypeTest {

  /** A row whose project_type is still NULL (pre-backfill) surfaces as CASUAL, never null. */
  @Test
  void from_nullProjectType_coalescesToCasual() {
    Project p = new Project();
    p.setCode("X");
    p.setName("X");
    p.setOwnerUserId(1L);
    // projectType left null
    assertEquals("CASUAL", ProjectDetail.from(p).getProjectType());
  }

  /** A set value passes through unchanged. */
  @Test
  void from_presentProjectType_passesThrough() {
    Project p = new Project();
    p.setProjectType("EXTERNAL_DELIVERY");
    assertEquals("EXTERNAL_DELIVERY", ProjectDetail.from(p).getProjectType());
  }
}
