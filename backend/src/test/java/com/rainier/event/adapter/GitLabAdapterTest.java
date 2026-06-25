/* (C) 2026 Rainier — internal use only. */
package com.rainier.event.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.rainier.event.domain.Event;
import com.rainier.event.extractor.ExtractionResult;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link GitLabAdapter} (v0.0.66). */
class GitLabAdapterTest {

  private final GitLabAdapter adapter = new GitLabAdapter();

  @Test
  void supports_onlyGitLab() {
    Event e = new Event();
    e.setSourceType("GITLAB");
    assertThat(adapter.supports(e)).isTrue();

    e.setSourceType("DINGTALK");
    assertThat(adapter.supports(e)).isFalse();

    assertThat(adapter.supports(null)).isFalse();
  }

  @Test
  void extract_payloadWithRaRef_returnsTaskRef() {
    Event e = new Event();
    e.setSourceType("GITLAB");
    e.setPayload("fix login RA-123 done");

    Optional<ExtractionResult> r = adapter.extract(e);

    assertThat(r).isPresent();
    assertThat(r.get().getEntityType()).isEqualTo("TASK");
    assertThat(r.get().getEntityId()).isEqualTo(123L);
    assertThat(r.get().getAction()).isEqualTo("COMMIT_REF");
  }

  @Test
  void extract_payloadWithoutRaRef_returnsEmpty() {
    Event e = new Event();
    e.setSourceType("GITLAB");
    e.setPayload("just a plain commit message");

    assertThat(adapter.extract(e)).isEmpty();
  }

  @Test
  void extract_nullPayload_returnsEmpty() {
    Event e = new Event();
    e.setSourceType("GITLAB");
    e.setPayload(null);

    assertThat(adapter.extract(e)).isEmpty();
  }
}
