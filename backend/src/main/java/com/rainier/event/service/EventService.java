/* (C) 2026 Rainier — internal use only. */
package com.rainier.event.service;

import com.rainier.event.domain.Event;
import com.rainier.event.extractor.EventExtractor;
import com.rainier.event.extractor.ExtractionResult;
import com.rainier.event.repository.EventRepository;
import com.rainier.event.sync.StatusSyncService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business operations for {@link Event} (v0.0.65, flywheel pipeline base). Two responsibilities:
 *
 * <ol>
 *   <li>{@link #record}: persist a raw external event verbatim (processed=false).
 *   <li>{@link #process}: walk the oldest unprocessed events, invoke the first matching {@link
 *       EventExtractor}, and write the extracted entity ref back. Events without any matching
 *       extractor are still marked processed=true to prevent re-processing loops.
 * </ol>
 */
@Service
@Transactional(readOnly = true)
public class EventService {

  private final EventRepository repo;
  private final List<EventExtractor> extractors;
  private final StatusSyncService statusSync;

  public EventService(
      EventRepository repo, List<EventExtractor> extractors, StatusSyncService statusSync) {
    this.repo = repo;
    this.extractors = extractors == null ? Collections.<EventExtractor>emptyList() : extractors;
    this.statusSync = statusSync;
  }

  @Transactional
  public Event record(
      String sourceType,
      String sourceId,
      String eventKind,
      String payload,
      LocalDateTime occurredAt) {
    Event e = new Event();
    e.setSourceType(sourceType);
    e.setSourceId(sourceId);
    e.setEventKind(eventKind);
    e.setPayload(payload);
    e.setOccurredAt(occurredAt);
    e.setProcessed(Boolean.FALSE);
    return repo.saveAndFlush(e);
  }

  /**
   * Drain up to {@code maxBatch} unprocessed events through the extractor chain. Returns the count
   * of events processed (regardless of whether an extractor matched).
   */
  @Transactional
  public int process(int maxBatch) {
    int cap = maxBatch <= 0 ? 0 : maxBatch;
    if (cap == 0) {
      return 0;
    }
    List<Event> pending =
        repo.findByProcessedFalseOrderByOccurredAtAsc(PageRequest.of(0, cap));
    List<Event> dirty = new ArrayList<Event>(pending.size());
    for (Event e : pending) {
      for (EventExtractor ex : extractors) {
        if (ex.supports(e)) {
          Optional<ExtractionResult> result = ex.extract(e);
          if (result != null && result.isPresent()) {
            ExtractionResult r = result.get();
            e.setExtractedEntityType(r.getEntityType());
            e.setExtractedEntityId(r.getEntityId());
          }
          break;
        }
      }
      e.setProcessed(Boolean.TRUE);
      dirty.add(e);
      if (statusSync != null) {
        statusSync.applyExtraction(e);
      }
    }
    if (!dirty.isEmpty()) {
      repo.saveAll(dirty);
      repo.flush();
    }
    return pending.size();
  }

  public List<Event> pending(int limit) {
    int cap = limit <= 0 ? 50 : limit;
    return repo.findByProcessedFalseOrderByOccurredAtAsc(PageRequest.of(0, cap));
  }
}
