/* (C) 2026 Rainier — internal use only. */
package com.rainier.event.bootstrap;

import com.rainier.event.repository.EventRepository;
import com.rainier.event.service.EventService;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * v0.0.66 — seed 5 sample events on startup (one per source type) so the flywheel pipeline has
 * data to demo. Gated on {@code app.demo.event-seed.enabled} (false in the test profile so tests
 * get a clean rainier_event table) and idempotent (only seeds when the table is empty).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EventSeed implements CommandLineRunner {

  private static final Logger LOG = LoggerFactory.getLogger(EventSeed.class);

  private final boolean enabled;
  private final EventRepository repo;
  private final EventService eventService;

  public EventSeed(
      @Value("${app.demo.event-seed.enabled:true}") boolean enabled,
      EventRepository repo,
      EventService eventService) {
    this.enabled = enabled;
    this.repo = repo;
    this.eventService = eventService;
  }

  @Override
  @Transactional
  public void run(String... args) {
    if (!enabled) {
      return;
    }
    if (repo.count() > 0) {
      return; // idempotent
    }
    LocalDateTime now = LocalDateTime.now();
    eventService.record(
        "GITLAB",
        "mr-1001",
        "PR_MERGE",
        "{\"title\":\"fix login bug RA-42\",\"author\":\"alice\"}",
        now.minusMinutes(20));
    eventService.record(
        "DINGTALK",
        "msg-2001",
        "MESSAGE",
        "{\"text\":\"今日站会同步\",\"from\":\"bob\"}",
        now.minusMinutes(15));
    eventService.record(
        "FEISHU",
        "doc-3001",
        "DOC_CHANGE",
        "{\"doc\":\"PRD-登录改造\",\"editor\":\"carol\"}",
        now.minusMinutes(10));
    eventService.record(
        "EMAIL",
        "mail-4001",
        "OTHER",
        "{\"subject\":\"上游接口变更通知\",\"from\":\"vendor@x\"}",
        now.minusMinutes(5));
    eventService.record(
        "ZENTAO",
        "zt-5001",
        "OTHER",
        "{\"text\":\"reopen bug-7 reason: regression\"}",
        now.minusMinutes(1));
    LOG.info("EventSeed: seeded 5 sample events across 5 source types");
  }
}
