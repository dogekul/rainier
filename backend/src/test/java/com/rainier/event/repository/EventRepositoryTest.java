/* (C) 2026 Rainier — internal use only. */
package com.rainier.event.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.rainier.event.domain.Event;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link EventRepository} (v0.0.65). */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EventRepositoryTest {

  @Autowired private EventRepository repo;

  @BeforeEach
  void cleanDb() {
    repo.deleteAll();
  }

  private Event seed(String sourceType, String sourceId, boolean processed, LocalDateTime occurredAt) {
    Event e = new Event();
    e.setSourceType(sourceType);
    e.setSourceId(sourceId);
    e.setEventKind("OTHER");
    e.setPayload("{}");
    e.setOccurredAt(occurredAt);
    e.setProcessed(processed);
    return repo.saveAndFlush(e);
  }

  /** TC-EVT-R-001: findByProcessedFalseOrderByOccurredAtAsc returns only unprocessed, oldest first. */
  @Test
  void findUnprocessed_returnsOnlyPendingOldestFirst() {
    LocalDateTime now = LocalDateTime.now();
    seed("GITLAB", "a", true, now.minusMinutes(5));
    seed("GITLAB", "b", false, now.minusMinutes(3));
    seed("GITLAB", "c", false, now.minusMinutes(7));

    List<Event> pending = repo.findByProcessedFalseOrderByOccurredAtAsc(PageRequest.of(0, 10));

    assertThat(pending).hasSize(2);
    assertThat(pending.get(0).getSourceId()).isEqualTo("c"); // -7m, oldest
    assertThat(pending.get(1).getSourceId()).isEqualTo("b"); // -3m
  }

  /** TC-EVT-R-002: findBySourceTypeAndSourceId scopes by both fields. */
  @Test
  void findBySourceTypeAndSourceId_scopesByBoth() {
    LocalDateTime now = LocalDateTime.now();
    seed("GITLAB", "mr-42", false, now);
    seed("DINGTALK", "mr-42", false, now);
    seed("GITLAB", "mr-99", false, now);

    List<Event> hit = repo.findBySourceTypeAndSourceId("GITLAB", "mr-42");

    assertThat(hit).hasSize(1);
    assertThat(hit.get(0).getSourceType()).isEqualTo("GITLAB");
    assertThat(hit.get(0).getSourceId()).isEqualTo("mr-42");
  }

  /** TC-EVT-R-003: persist assigns id + receivedAt auto. */
  @Test
  void save_assignsIdAndReceivedAt() {
    Event e = seed("GITLAB", "x", false, LocalDateTime.now());
    assertThat(e.getId()).isNotNull().isPositive();
    assertThat(e.getReceivedAt()).isNotNull();
    assertThat(e.getProcessed()).isFalse();
  }
}
