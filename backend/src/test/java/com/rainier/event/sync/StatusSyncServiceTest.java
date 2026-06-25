/* (C) 2026 Rainier — internal use only. */
package com.rainier.event.sync;

import static org.assertj.core.api.Assertions.assertThat;

import com.rainier.aiworklog.domain.AiWorkLog;
import com.rainier.aiworklog.domain.AiWorkLogStatus;
import com.rainier.aiworklog.repository.AiWorkLogRepository;
import com.rainier.event.domain.Event;
import com.rainier.event.repository.EventRepository;
import com.rainier.event.service.EventService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * v0.0.67 — verifies the Event → AiWorkLog PROPOSED bridge. Uses the live extractor chain so the
 * full pipeline (record → process → status-sync) is exercised end-to-end on H2.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StatusSyncServiceTest {

  @Autowired private StatusSyncService statusSync;
  @Autowired private EventService eventService;
  @Autowired private EventRepository eventRepo;
  @Autowired private AiWorkLogRepository aiWorkLogRepo;

  @BeforeEach
  void cleanDb() {
    aiWorkLogRepo.deleteAll();
    eventRepo.deleteAll();
  }

  /** TC-SAS-S-001: PR_MERGE + TASK extracted → 1 PROPOSED AiWorkLog. */
  @Test
  void applyExtraction_prMergeWithTaskRef_createsProposed() {
    long before = aiWorkLogRepo.count();

    Event e = new Event();
    e.setSourceType("GITLAB");
    e.setSourceId("mr-42");
    e.setEventKind("PR_MERGE");
    e.setPayload("fix login RA-42 done");
    e.setOccurredAt(LocalDateTime.now());
    e.setProcessed(Boolean.TRUE);
    e.setExtractedEntityType("TASK");
    e.setExtractedEntityId(42L);
    eventRepo.saveAndFlush(e);

    statusSync.applyExtraction(e);

    assertThat(aiWorkLogRepo.count()).isEqualTo(before + 1);
    List<AiWorkLog> all = aiWorkLogRepo.findAll();
    AiWorkLog log = all.get(all.size() - 1);
    assertThat(log.getAgentType()).isEqualTo("STATUS_SYNC");
    assertThat(log.getAction()).isEqualTo("UPDATE_TASK_STATUS");
    assertThat(log.getTargetType()).isEqualTo("TASK");
    assertThat(log.getTargetId()).isEqualTo(42L);
    assertThat(log.getStatus()).isEqualTo(AiWorkLogStatus.PROPOSED);
    assertThat(log.getSummary()).contains("#42").contains("DONE");
    assertThat(log.getEvidence()).contains("\"eventId\":").contains("PR_MERGE").contains("GITLAB");
  }

  /** TC-SAS-S-002: Non-PR_MERGE event → no AiWorkLog. */
  @Test
  void applyExtraction_nonPrMerge_noOp() {
    long before = aiWorkLogRepo.count();

    Event e = new Event();
    e.setSourceType("GITLAB");
    e.setEventKind("PR_OPEN");
    e.setOccurredAt(LocalDateTime.now());
    e.setProcessed(Boolean.TRUE);
    e.setExtractedEntityType("TASK");
    e.setExtractedEntityId(42L);
    eventRepo.saveAndFlush(e);

    statusSync.applyExtraction(e);

    assertThat(aiWorkLogRepo.count()).isEqualTo(before);
  }

  /** TC-SAS-S-003: Non-TASK extraction (e.g. STORY) → no AiWorkLog. */
  @Test
  void applyExtraction_nonTaskRef_noOp() {
    long before = aiWorkLogRepo.count();

    Event e = new Event();
    e.setSourceType("ZENTAO");
    e.setEventKind("PR_MERGE");
    e.setOccurredAt(LocalDateTime.now());
    e.setProcessed(Boolean.TRUE);
    e.setExtractedEntityType("STORY");
    e.setExtractedEntityId(7L);
    eventRepo.saveAndFlush(e);

    statusSync.applyExtraction(e);

    assertThat(aiWorkLogRepo.count()).isEqualTo(before);
  }

  /** TC-SAS-S-004: processed=false → no AiWorkLog (guard). */
  @Test
  void applyExtraction_notProcessed_noOp() {
    long before = aiWorkLogRepo.count();

    Event e = new Event();
    e.setSourceType("GITLAB");
    e.setEventKind("PR_MERGE");
    e.setOccurredAt(LocalDateTime.now());
    e.setProcessed(Boolean.FALSE);
    e.setExtractedEntityType("TASK");
    e.setExtractedEntityId(42L);
    eventRepo.saveAndFlush(e);

    statusSync.applyExtraction(e);

    assertThat(aiWorkLogRepo.count()).isEqualTo(before);
  }

  /** TC-SAS-S-005: full pipeline — record GitLab PR_MERGE → process → 1 PROPOSED for task 100. */
  @Test
  void process_endToEnd_createsProposedForExtractedTask() {
    long before = aiWorkLogRepo.count();

    Event recorded =
        eventService.record(
            "GITLAB", "mr-100", "PR_MERGE", "fix login RA-100 done", LocalDateTime.now());

    int processed = eventService.process(10);

    assertThat(processed).isEqualTo(1);
    Event after =
        eventRepo
            .findById(recorded.getId())
            .orElseThrow(() -> new AssertionError("event missing"));
    assertThat(after.getProcessed()).isTrue();
    assertThat(after.getExtractedEntityType()).isEqualTo("TASK");
    assertThat(after.getExtractedEntityId()).isEqualTo(100L);

    assertThat(aiWorkLogRepo.count()).isEqualTo(before + 1);
    List<AiWorkLog> all = aiWorkLogRepo.findAll();
    AiWorkLog log = all.get(all.size() - 1);
    assertThat(log.getAgentType()).isEqualTo("STATUS_SYNC");
    assertThat(log.getTargetId()).isEqualTo(100L);
    assertThat(log.getStatus()).isEqualTo(AiWorkLogStatus.PROPOSED);
  }
}
