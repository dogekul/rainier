/* (C) 2026 Rainier — internal use only. */
package com.rainier.event.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.rainier.event.domain.Event;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link FeishuAdapter} (v0.0.66). */
class FeishuAdapterTest {

  private final FeishuAdapter adapter = new FeishuAdapter();

  @Test
  void supports_onlyFeishu() {
    Event e = new Event();
    e.setSourceType("FEISHU");
    assertThat(adapter.supports(e)).isTrue();

    e.setSourceType("EMAIL");
    assertThat(adapter.supports(e)).isFalse();
  }

  @Test
  void extract_alwaysEmpty() {
    Event e = new Event();
    e.setSourceType("FEISHU");
    e.setPayload("doc changed");

    assertThat(adapter.extract(e)).isEmpty();
  }
}
