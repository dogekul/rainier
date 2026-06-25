/* (C) 2026 Rainier — internal use only. */
package com.rainier.event.repository;

import com.rainier.event.domain.Event;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/** Repository for {@link Event} (v0.0.65, flywheel pipeline base). */
@Repository
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

  /** Oldest-first batch of unprocessed events (FIFO processing). */
  List<Event> findByProcessedFalseOrderByOccurredAtAsc(Pageable pageable);

  /** Idempotency probe: lookup by (sourceType, sourceId). */
  List<Event> findBySourceTypeAndSourceId(String sourceType, String sourceId);
}
