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
 * v0.0.66 — GitLab {@link EventExtractor} stub. Matches sourceType="GITLAB" and tries to find a
 * Rainier task ref like {@code RA-123} in payload. Found → {@code (TASK, 123, "COMMIT_REF")};
 * not-found → empty (still flagged processed by EventService). No real GitLab API call.
 */
@Component
public class GitLabAdapter implements EventExtractor {

  /** Matches RA-<digits>, captures the digits. */
  private static final Pattern TASK_REF = Pattern.compile("RA-(\\d+)");

  @Override
  public boolean supports(Event event) {
    return event != null && "GITLAB".equals(event.getSourceType());
  }

  @Override
  public Optional<ExtractionResult> extract(Event event) {
    if (event == null || event.getPayload() == null) {
      return Optional.empty();
    }
    Matcher m = TASK_REF.matcher(event.getPayload());
    if (!m.find()) {
      return Optional.empty();
    }
    try {
      Long taskId = Long.valueOf(m.group(1));
      return Optional.of(new ExtractionResult("TASK", taskId, "COMMIT_REF"));
    } catch (NumberFormatException nfe) {
      return Optional.empty();
    }
  }
}
