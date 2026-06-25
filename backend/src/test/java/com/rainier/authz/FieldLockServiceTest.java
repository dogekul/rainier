/* (C) 2026 Rainier — internal use only. */
package com.rainier.authz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rainier.common.exception.BadRequestException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Service-level tests for {@link FieldLockService} (v0.0.69, A5). */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FieldLockServiceTest {

  @Autowired private FieldLockService service;
  @Autowired private FieldLockRepository repo;

  @BeforeEach
  void cleanDb() {
    repo.deleteAll();
  }

  @Test
  void lock_persistsRowWithLockedAt() {
    FieldLock l = service.lock("TASK", 42L, "status", "USER");
    assertThat(l.getId()).isNotNull();
    assertThat(l.getEntityType()).isEqualTo("TASK");
    assertThat(l.getEntityId()).isEqualTo(42L);
    assertThat(l.getFieldName()).isEqualTo("status");
    assertThat(l.getLockedBy()).isEqualTo("USER");
    assertThat(l.getLockedAt()).isNotNull();
  }

  @Test
  void lock_isIdempotent_returnsExistingRow() {
    FieldLock first = service.lock("TASK", 42L, "status", "USER");
    FieldLock second = service.lock("TASK", 42L, "status", "AI");

    assertThat(second.getId()).isEqualTo(first.getId());
    // lockedBy from the original row is preserved (idempotent lock does NOT overwrite metadata).
    assertThat(second.getLockedBy()).isEqualTo("USER");
    assertThat(repo.count()).isEqualTo(1L);
  }

  @Test
  void unlock_removesRow_andIsIdempotent() {
    service.lock("TASK", 1L, "title", "USER");
    assertThat(repo.count()).isEqualTo(1L);

    service.unlock("TASK", 1L, "title");
    assertThat(repo.count()).isEqualTo(0L);

    // Second call must not throw (idempotent).
    service.unlock("TASK", 1L, "title");
    assertThat(repo.count()).isEqualTo(0L);
  }

  @Test
  void listFor_returnsAllLocksForEntity() {
    service.lock("TASK", 7L, "status", "USER");
    service.lock("TASK", 7L, "priority", "AI");
    service.lock("TASK", 8L, "status", "USER");
    service.lock("STORY", 7L, "status", "USER");

    List<FieldLock> locks = service.listFor("TASK", 7L);
    assertThat(locks).hasSize(2);
    assertThat(locks).extracting(FieldLock::getFieldName).containsExactlyInAnyOrder("status", "priority");
  }

  @Test
  void isLocked_reflectsRepoState() {
    assertThat(service.isLocked("TASK", 1L, "status")).isFalse();
    service.lock("TASK", 1L, "status", "USER");
    assertThat(service.isLocked("TASK", 1L, "status")).isTrue();
    assertThat(service.isLocked("TASK", 1L, "title")).isFalse();
  }

  @Test
  void lock_blankEntityType_throws400() {
    assertThatThrownBy(() -> service.lock(" ", 1L, "status", "USER"))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  void lock_nullEntityId_throws400() {
    assertThatThrownBy(() -> service.lock("TASK", null, "status", "USER"))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  void lock_blankFieldName_throws400() {
    assertThatThrownBy(() -> service.lock("TASK", 1L, "", "USER"))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  void lock_blankLockedBy_throws400() {
    assertThatThrownBy(() -> service.lock("TASK", 1L, "status", null))
        .isInstanceOf(BadRequestException.class);
  }
}
