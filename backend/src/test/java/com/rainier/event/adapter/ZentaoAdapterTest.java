/* (C) 2026 Rainier — internal use only. */
package com.rainier.event.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.rainier.event.domain.Event;
import com.rainier.event.extractor.ExtractionResult;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link ZentaoAdapter} (v0.0.66). */
class ZentaoAdapterTest {

  private final ZentaoAdapter adapter = new ZentaoAdapter();

  @Test
  void supports_onlyZentao() {
    Event e = new Event();
    e.setSourceType("ZENTAO");
    assertThat(adapter.supports(e)).isTrue();

    e.setSourceType("EMAIL");
    assertThat(adapter.supports(e)).isFalse();

    assertThat(adapter.supports(null)).isFalse();
  }

  @Test
  void extract_payloadWithBugRef_returnsStoryRef() {
    Event e = new Event();
    e.setSourceType("ZENTAO");
    e.setPayload("reopen bug-7 reason: regression");

    Optional<ExtractionResult> r = adapter.extract(e);

    assertThat(r).isPresent();
    assertThat(r.get().getEntityType()).isEqualTo("STORY");
    assertThat(r.get().getEntityId()).isEqualTo(7L);
    assertThat(r.get().getAction()).isEqualTo("BUG_REPORT");
  }

  @Test
  void extract_payloadWithoutBugRef_returnsEmpty() {
    Event e = new Event();
    e.setSourceType("ZENTAO");
    e.setPayload("normal status update");

    assertThat(adapter.extract(e)).isEmpty();
  }

  @Test
  void extract_nullPayload_returnsEmpty() {
    Event e = new Event();
    e.setSourceType("ZENTAO");

    assertThat(adapter.extract(e)).isEmpty();
  }
}
