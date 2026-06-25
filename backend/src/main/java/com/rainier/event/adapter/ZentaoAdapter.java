/* (C) 2026 Rainier — internal use only. */
package com.rainier.event.adapter;

import com.rainier.event.domain.Event;
import com.rainier.event.extractor.EventExtractor;
import com.rainier.event.extractor.ExtractionResult;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * v0.0.66 — 禅道 {@link EventExtractor} stub. Matches sourceType="ZENTAO" and tries to find a
 * Zentao bug ref like {@code bug-7} in payload. Found → {@code (STORY, 7, "BUG_REPORT")};
 * not-found → empty.
 */
@Component
public class ZentaoAdapter implements EventExtractor {

  /** Matches bug-<digits>, captures the digits. Case-insensitive. */
  private static final Pattern BUG_REF = Pattern.compile("bug-(\\d+)", Pattern.CASE_INSENSITIVE);

  @Override
  public boolean supports(Event event) {
    return event != null && "ZENTAO".equals(event.getSourceType());
  }

  @Override
  public Optional<ExtractionResult> extract(Event event) {
    if (event == null || event.getPayload() == null) {
      return Optional.empty();
    }
    Matcher m = BUG_REF.matcher(event.getPayload());
    if (!m.find()) {
      return Optional.empty();
    }
    try {
      Long storyId = Long.valueOf(m.group(1));
      return Optional.of(new ExtractionResult("STORY", storyId, "BUG_REPORT"));
    } catch (NumberFormatException nfe) {
      return Optional.empty();
    }
  }
}
