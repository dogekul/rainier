/* (C) 2026 Rainier — internal use only. */
package com.rainier.event.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.rainier.event.domain.Event;
import com.rainier.event.repository.EventRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * v0.0.66 — integration test that seeds 5 events across the 5 source types and verifies the
 * full production extractor chain (GitLab / DingTalk / Feishu / Email / Zentao) drains them all,
 * with GitLab + Zentao writing back entity refs.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EventServiceProcessIntegrationTest {

  @Autowired private EventService svc;
  @Autowired private EventRepository repo;

  @BeforeEach
  void cleanDb() {
    repo.deleteAll();
  }

  @Test
  void process_allFiveAdapters_drainsEverythingWithCorrectExtraction() {
    LocalDateTime now = LocalDateTime.now();
    Event gitlab =
        svc.record("GITLAB", "mr-1", "PR_MERGE", "fix login RA-42 done", now.minusMinutes(50));
    Event ding =
        svc.record("DINGTALK", "msg-1", "MESSAGE", "{\"text\":\"hi\"}", now.minusMinutes(40));
    Event feishu =
        svc.record("FEISHU", "doc-1", "DOC_CHANGE", "{\"doc\":\"x\"}", now.minusMinutes(30));
    Event email =
        svc.record("EMAIL", "mail-1", "OTHER", "{\"subject\":\"hi\"}", now.minusMinutes(20));
    Event zentao =
        svc.record(
            "ZENTAO", "zt-1", "OTHER", "reopen bug-7 reason: regression", now.minusMinutes(10));

    int processedCount = svc.process(50);
    assertThat(processedCount).isEqualTo(5);

    List<Event> all = repo.findAll();
    assertThat(all).hasSize(5);
    for (Event e : all) {
      assertThat(e.getProcessed()).isTrue();
    }

    Event afterGitlab =
        repo.findById(gitlab.getId()).orElseThrow(() -> new AssertionError("gitlab missing"));
    assertThat(afterGitlab.getExtractedEntityType()).isEqualTo("TASK");
    assertThat(afterGitlab.getExtractedEntityId()).isEqualTo(42L);

    Event afterZentao =
        repo.findById(zentao.getId()).orElseThrow(() -> new AssertionError("zentao missing"));
    assertThat(afterZentao.getExtractedEntityType()).isEqualTo("STORY");
    assertThat(afterZentao.getExtractedEntityId()).isEqualTo(7L);

    // 3 stubs: processed=true but no extraction.
    Event afterDing =
        repo.findById(ding.getId()).orElseThrow(() -> new AssertionError("ding missing"));
    Event afterFeishu =
        repo.findById(feishu.getId()).orElseThrow(() -> new AssertionError("feishu missing"));
    Event afterEmail =
        repo.findById(email.getId()).orElseThrow(() -> new AssertionError("email missing"));
    assertThat(afterDing.getExtractedEntityType()).isNull();
    assertThat(afterFeishu.getExtractedEntityType()).isNull();
    assertThat(afterEmail.getExtractedEntityType()).isNull();
  }
}
