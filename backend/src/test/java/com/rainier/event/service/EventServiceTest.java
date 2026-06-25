/* (C) 2026 Rainier — internal use only. */
package com.rainier.event.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.rainier.event.domain.Event;
import com.rainier.event.extractor.EventExtractor;
import com.rainier.event.extractor.ExtractionResult;
import com.rainier.event.repository.EventRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for {@link EventService} (v0.0.65). Uses real repo (H2 test profile) and a
 * hand-rolled {@link EventExtractor} list (the production extractor beans are stub in A1).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EventServiceTest {

  @Autowired private EventRepository repo;

  @BeforeEach
  void cleanDb() {
    repo.deleteAll();
  }

  /** Fake extractor: matches by sourceType, returns a fixed ExtractionResult (or empty). */
  private static final class FakeExtractor implements EventExtractor {
    private final String match;
    private final ExtractionResult result;

    FakeExtractor(String match, ExtractionResult result) {
      this.match = match;
      this.result = result;
    }

    @Override
    public boolean supports(Event event) {
      return match.equals(event.getSourceType());
    }

    @Override
    public Optional<ExtractionResult> extract(Event event) {
      return result == null ? Optional.<ExtractionResult>empty() : Optional.of(result);
    }
  }

  /** TC-EVT-S-001: record() persists with processed=false + receivedAt auto. */
  @Test
  void record_persistsWithProcessedFalse() {
    EventService svc = new EventService(repo, java.util.Collections.<EventExtractor>emptyList());

    LocalDateTime occ = LocalDateTime.now().minusMinutes(1);
    Event saved = svc.record("GITLAB", "mr-1", "PR_OPEN", "{\"x\":1}", occ);

    assertThat(saved.getId()).isNotNull().isPositive();
    assertThat(saved.getProcessed()).isFalse();
    assertThat(saved.getSourceType()).isEqualTo("GITLAB");
    assertThat(saved.getReceivedAt()).isNotNull();
    assertThat(saved.getOccurredAt()).isEqualTo(occ);
  }

  /** TC-EVT-S-002: process() with matching extractor writes back entity ref. */
  @Test
  void process_matchingExtractor_writesEntityRef() {
    EventExtractor matchTask =
        new FakeExtractor("GITLAB", new ExtractionResult("TASK", 123L, "UPDATE"));
    EventService svc = new EventService(repo, java.util.Arrays.asList(matchTask));

    Event e = svc.record("GITLAB", "mr-1", "PR_OPEN", "{}", LocalDateTime.now());
    Long id = e.getId();

    int processed = svc.process(10);

    assertThat(processed).isEqualTo(1);
    Event after = repo.findById(id).orElseThrow(() -> new AssertionError("event missing"));
    assertThat(after.getProcessed()).isTrue();
    assertThat(after.getExtractedEntityType()).isEqualTo("TASK");
    assertThat(after.getExtractedEntityId()).isEqualTo(123L);
  }

  /** TC-EVT-S-003: process() with no matching extractor still flips processed=true. */
  @Test
  void process_noMatchingExtractor_stillMarksProcessed() {
    EventExtractor onlyDing =
        new FakeExtractor("DINGTALK", new ExtractionResult("STORY", 1L, "NOTE"));
    EventService svc = new EventService(repo, java.util.Arrays.asList(onlyDing));

    Event e = svc.record("GITLAB", "mr-1", "PR_OPEN", "{}", LocalDateTime.now());
    Long id = e.getId();

    int processed = svc.process(10);

    assertThat(processed).isEqualTo(1);
    Event after = repo.findById(id).orElseThrow(() -> new AssertionError("event missing"));
    assertThat(after.getProcessed()).isTrue();
    assertThat(after.getExtractedEntityType()).isNull();
    assertThat(after.getExtractedEntityId()).isNull();
  }

  /** TC-EVT-S-004: process() respects oldest-first FIFO via occurredAt. */
  @Test
  void process_drainsOldestFirst() {
    EventService svc = new EventService(repo, java.util.Collections.<EventExtractor>emptyList());
    LocalDateTime now = LocalDateTime.now();
    svc.record("GITLAB", "newer", "PR_OPEN", "{}", now.minusMinutes(1));
    svc.record("GITLAB", "older", "PR_OPEN", "{}", now.minusMinutes(5));

    svc.process(1);

    List<Event> stillPending = svc.pending(10);
    assertThat(stillPending).hasSize(1);
    assertThat(stillPending.get(0).getSourceId()).isEqualTo("newer");
  }

  /** TC-EVT-S-005: process(0) is a no-op. */
  @Test
  void process_zeroBatch_isNoOp() {
    EventService svc = new EventService(repo, java.util.Collections.<EventExtractor>emptyList());
    svc.record("GITLAB", "x", "PR_OPEN", "{}", LocalDateTime.now());

    assertThat(svc.process(0)).isEqualTo(0);
    assertThat(svc.pending(10)).hasSize(1);
  }
}
