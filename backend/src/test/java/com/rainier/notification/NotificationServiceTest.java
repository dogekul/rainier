/* (C) 2026 Rainier — internal use only. */
package com.rainier.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.rainier.notification.domain.Notification;
import com.rainier.notification.repository.NotificationRepository;
import com.rainier.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

/** v0.0.72 (A8) — NotificationService 行为验证：send / listFor / markRead / markAllRead. */
@SpringBootTest
@ActiveProfiles("test")
class NotificationServiceTest {

  @Autowired private NotificationService service;
  @Autowired private NotificationRepository repo;

  private static final Long ALICE = 1001L;
  private static final Long BOB = 1002L;

  @BeforeEach
  void clean() {
    repo.deleteAll();
  }

  /** Scenario 1 — send 写入一条 INFO 通知；createdAt 非空，readAt 为 null. */
  @Test
  void send_persistsNotificationWithCreatedAtAndNullReadAt() {
    Notification n = service.send(ALICE, "标题", "正文", "INFO", "TASK", 42L);
    assertThat(n.getId()).isNotNull();
    assertThat(n.getUserId()).isEqualTo(ALICE);
    assertThat(n.getTitle()).isEqualTo("标题");
    assertThat(n.getLevel()).isEqualTo("INFO");
    assertThat(n.getEntityType()).isEqualTo("TASK");
    assertThat(n.getEntityId()).isEqualTo(42L);
    assertThat(n.getCreatedAt()).isNotNull();
    assertThat(n.getReadAt()).isNull();
  }

  /** Scenario 2 — listFor onlyUnread=true 仅返回未读. */
  @Test
  void listFor_onlyUnread_returnsUnreadOnly() {
    Notification n1 = service.send(ALICE, "n1", null, "INFO", null, null);
    Notification n2 = service.send(ALICE, "n2", null, "INFO", null, null);
    service.send(ALICE, "n3", null, "INFO", null, null);
    service.markRead(n2.getId(), ALICE);

    Page<Notification> unread = service.listFor(ALICE, PageRequest.of(0, 20), true);
    assertThat(unread.getTotalElements()).isEqualTo(2L);
    assertThat(unread.getContent())
        .extracting(Notification::getTitle)
        .containsExactlyInAnyOrder("n1", "n3");

    Page<Notification> all = service.listFor(ALICE, PageRequest.of(0, 20), false);
    assertThat(all.getTotalElements()).isEqualTo(3L);
    // sanity: n1 still id present
    assertThat(all.getContent()).extracting(Notification::getId).contains(n1.getId());
  }

  /** Scenario 3 — markAllRead 仅影响当前用户. */
  @Test
  void markAllRead_onlyAffectsTargetUser() {
    service.send(ALICE, "a1", null, "INFO", null, null);
    service.send(ALICE, "a2", null, "WARN", null, null);
    Notification bobN = service.send(BOB, "b1", null, "CRIT", null, null);

    int updated = service.markAllRead(ALICE);
    assertThat(updated).isEqualTo(2);

    Page<Notification> aliceUnread = service.listFor(ALICE, PageRequest.of(0, 20), true);
    assertThat(aliceUnread.getTotalElements()).isEqualTo(0L);

    Page<Notification> bobUnread = service.listFor(BOB, PageRequest.of(0, 20), true);
    assertThat(bobUnread.getTotalElements()).isEqualTo(1L);
    assertThat(bobUnread.getContent().get(0).getId()).isEqualTo(bobN.getId());
  }

  /** markRead 幂等：再次调用 readAt 不变. */
  @Test
  void markRead_isIdempotent() {
    Notification n = service.send(ALICE, "x", null, "INFO", null, null);
    Notification first = service.markRead(n.getId(), ALICE);
    assertThat(first.getReadAt()).isNotNull();
    java.time.LocalDateTime ts = first.getReadAt();
    Notification second = service.markRead(n.getId(), ALICE);
    assertThat(second.getReadAt()).isEqualTo(ts);
  }
}
