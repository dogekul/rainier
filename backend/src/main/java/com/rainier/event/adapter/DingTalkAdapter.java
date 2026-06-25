/* (C) 2026 Rainier — internal use only. */
package com.rainier.event.adapter;

import com.rainier.event.domain.Event;
import com.rainier.event.extractor.EventExtractor;
import com.rainier.event.extractor.ExtractionResult;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * v0.0.66 — 钉钉 {@link EventExtractor} stub. Matches sourceType="DINGTALK" but {@code extract}
 * always returns empty (no semantic parsing in A2). EventService will still flip processed=true.
 */
@Component
public class DingTalkAdapter implements EventExtractor {

  @Override
  public boolean supports(Event event) {
    return event != null && "DINGTALK".equals(event.getSourceType());
  }

  @Override
  public Optional<ExtractionResult> extract(Event event) {
    return Optional.empty();
  }
}
