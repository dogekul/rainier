/* (C) 2026 Rainier — internal use only. */
package com.rainier.requirement.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.rainier.requirement.domain.Requirement;
import com.rainier.requirement.repository.RequirementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** TC-REQE-003 — startup remap of legacy requirement statuses to the v0.0.19 set. */
@SpringBootTest
@ActiveProfiles("test")
class RequirementStatusBackfillTest {

  @Autowired private RequirementStatusBackfill backfill;
  @Autowired private RequirementRepository repo;

  @BeforeEach
  void cleanDb() {
    repo.deleteAll();
  }

  private Long seed(String code, String status) {
    Requirement r = new Requirement();
    r.setCode(code);
    r.setTitle("T-" + code);
    r.setOwnerUserId(1L);
    r.setStatus(status);
    r.setPriority("MEDIUM");
    return repo.saveAndFlush(r).getId();
  }

  private Requirement reload(Long id) {
    return repo.findById(id).orElseThrow(IllegalStateException::new);
  }

  /** Legacy statuses remap to the new set; DRAFT/DELIVERED untouched; other fields unchanged. */
  @Test
  void run_remapsLegacyStatuses() {
    Long a = seed("R-IR", "IN_REVIEW");
    Long b = seed("R-AP", "APPROVED");
    Long c = seed("R-ID", "IN_DEV");
    Long d = seed("R-DP", "DEPRECATED");
    Long e = seed("R-DR", "DRAFT");
    Long f = seed("R-DV", "DELIVERED");

    backfill.run();

    assertEquals("IN_APPROVAL", reload(a).getStatus());
    assertEquals("IN_ANALYSIS", reload(b).getStatus());
    assertEquals("IN_PROGRESS", reload(c).getStatus());
    assertEquals("CLOSED", reload(d).getStatus());
    assertEquals("DRAFT", reload(e).getStatus());
    assertEquals("DELIVERED", reload(f).getStatus());

    // The remapped row keeps everything else.
    Requirement ra = reload(a);
    assertEquals("R-IR", ra.getCode());
    assertEquals("T-R-IR", ra.getTitle());
    assertEquals("MEDIUM", ra.getPriority());
    // An untouched (DRAFT) row is also fully intact.
    Requirement re = reload(e);
    assertEquals("R-DR", re.getCode());
    assertEquals("T-R-DR", re.getTitle());
    assertEquals("MEDIUM", re.getPriority());
  }
}
