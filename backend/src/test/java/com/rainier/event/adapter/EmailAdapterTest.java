/* (C) 2026 Rainier — internal use only. */
package com.rainier.event.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.rainier.event.domain.Event;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link EmailAdapter} (v0.0.66). */
class EmailAdapterTest {

  private final EmailAdapter adapter = new EmailAdapter();

  @Test
  void supports_onlyEmail() {
    Event e = new Event();
    e.setSourceType("EMAIL");
    assertThat(adapter.supports(e)).isTrue();

    e.setSourceType("ZENTAO");
    assertThat(adapter.supports(e)).isFalse();
  }

  @Test
  void extract_alwaysEmpty() {
    Event e = new Event();
    e.setSourceType("EMAIL");
    e.setPayload("subject foo");

    assertThat(adapter.extract(e)).isEmpty();
  }
}
