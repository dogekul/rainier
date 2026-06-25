/* (C) 2026 Rainier — internal use only. */
package com.rainier.event.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.rainier.event.domain.Event;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link DingTalkAdapter} (v0.0.66). */
class DingTalkAdapterTest {

  private final DingTalkAdapter adapter = new DingTalkAdapter();

  @Test
  void supports_onlyDingTalk() {
    Event e = new Event();
    e.setSourceType("DINGTALK");
    assertThat(adapter.supports(e)).isTrue();

    e.setSourceType("GITLAB");
    assertThat(adapter.supports(e)).isFalse();
  }

  @Test
  void extract_alwaysEmpty() {
    Event e = new Event();
    e.setSourceType("DINGTALK");
    e.setPayload("any payload");

    assertThat(adapter.extract(e)).isEmpty();
  }
}
