/* (C) 2026 Rainier — internal use only. */
package com.rainier.common.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import org.junit.jupiter.api.Test;

/**
 * Reflection-level guard for the v1→v2 id strategy migration (TC-MIG-001).
 *
 * <p>The id field of {@link BaseEntity} must be {@code Long} (not {@code String}) and must be
 * annotated with {@code @GeneratedValue(strategy = IDENTITY)}.
 */
class BaseEntityReflectionTest {

  @Test
  void idField_typeIsLong_andStrategyIsIdentity() throws NoSuchFieldException {
    Field idField = BaseEntity.class.getDeclaredField("id");
    assertThat(idField.getType()).isEqualTo(Long.class);

    GeneratedValue generatedValue = idField.getAnnotation(GeneratedValue.class);
    assertThat(generatedValue).as("@GeneratedValue must be present on BaseEntity.id").isNotNull();
    assertThat(generatedValue.strategy()).isEqualTo(GenerationType.IDENTITY);
  }
}
