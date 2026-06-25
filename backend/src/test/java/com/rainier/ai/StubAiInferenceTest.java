/* (C) 2026 Rainier — internal use only. */
package com.rainier.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Unit tests for {@link StubAiInference} (v0.0.68, A4). Deterministic stub by outputClass. */
class StubAiInferenceTest {

  private final StubAiInference stub = new StubAiInference();

  @Test
  void infer_string_returnsStubPlusKind() {
    String out = stub.infer("ANY_KIND", "input", String.class);
    assertThat(out).isEqualTo("stub:ANY_KIND");
  }

  @Test
  void infer_string_nullKind_usesUnknown() {
    String out = stub.infer(null, null, String.class);
    assertThat(out).isEqualTo("stub:UNKNOWN");
  }

  @Test
  void infer_integer_returnsZero() {
    Integer out = stub.infer("KIND", null, Integer.class);
    assertThat(out).isEqualTo(0);
  }

  @Test
  void infer_long_returnsZero() {
    Long out = stub.infer("KIND", null, Long.class);
    assertThat(out).isEqualTo(0L);
  }

  @Test
  void infer_boolean_returnsFalse() {
    Boolean out = stub.infer("KIND", null, Boolean.class);
    assertThat(out).isEqualTo(Boolean.FALSE);
  }

  @Test
  void infer_pojo_returnsDefaultInstance() {
    Bag out = stub.infer("KIND", null, Bag.class);
    assertThat(out).isNotNull();
    assertThat(out.value).isNull();
  }

  @Test
  void infer_nullOutputClass_throws() {
    assertThatThrownBy(() -> stub.infer("KIND", null, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void infer_uninstantiable_throws() {
    assertThatThrownBy(() -> stub.infer("KIND", null, NoDefaultCtor.class))
        .isInstanceOf(IllegalArgumentException.class);
  }

  public static class Bag {
    public String value;
  }

  public static class NoDefaultCtor {
    @SuppressWarnings("unused")
    public NoDefaultCtor(String required) {}
  }
}
