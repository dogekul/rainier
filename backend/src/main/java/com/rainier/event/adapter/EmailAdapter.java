/* (C) 2026 Rainier — internal use only. */
package com.rainier.event.adapter;

import com.rainier.event.domain.Event;
import com.rainier.event.extractor.EventExtractor;
import com.rainier.event.extractor.ExtractionResult;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * v0.0.66 — 邮件 {@link EventExtractor} stub. Matches sourceType="EMAIL" but {@code extract}
 * always returns empty.
 */
@Component
public class EmailAdapter implements EventExtractor {

  @Override
  public boolean supports(Event event) {
    return event != null && "EMAIL".equals(event.getSourceType());
  }

  @Override
  public Optional<ExtractionResult> extract(Event event) {
    return Optional.empty();
  }
}
