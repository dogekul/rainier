/* (C) 2026 Rainier — internal use only. */
package com.rainier.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rainier.ai.domain.AiErrorStatus;
import com.rainier.ai.dto.AiErrorDetail;
import com.rainier.ai.repository.AiErrorRepository;
import com.rainier.ai.service.AiErrorService;
import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.ConflictException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Service-level tests for {@link AiErrorService} (v0.0.68, A4). */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AiErrorServiceTest {

  @Autowired private AiErrorService service;
  @Autowired private AiErrorRepository repo;

  @BeforeEach
  void cleanDb() {
    repo.deleteAll();
  }

  @Test
  void record_returnsOpenWithOccurredAt() {
    AiErrorDetail d =
        service.record(
            "SYNC_TASK_STATUS", "状态推理错误", "TASK", 42L, "MODEL", "evidence text");
    assertThat(d.getId()).isNotNull();
    assertThat(d.getStatus()).isEqualTo(AiErrorStatus.OPEN);
    assertThat(d.getOccurredAt()).isNotNull();
    assertThat(d.getAiAction()).isEqualTo("SYNC_TASK_STATUS");
    assertThat(d.getAffectedEntityType()).isEqualTo("TASK");
    assertThat(d.getAffectedEntityId()).isEqualTo(42L);
    assertThat(d.getRootCause()).isEqualTo("MODEL");
    assertThat(d.getEvidence()).isEqualTo("evidence text");
  }

  @Test
  void record_blankAction_throws400() {
    assertThatThrownBy(() -> service.record(" ", "desc", null, null, null, null))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  void record_blankDesc_throws400() {
    assertThatThrownBy(() -> service.record("ACT", " ", null, null, null, null))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  void markFixed_transitionsToFixed() {
    AiErrorDetail open = service.record("ACT", "desc", null, null, null, null);
    AiErrorDetail fixed = service.markFixed(open.getId(), "调整 prompt v2");
    assertThat(fixed.getStatus()).isEqualTo(AiErrorStatus.FIXED);
    assertThat(fixed.getFixAction()).isEqualTo("调整 prompt v2");
  }

  @Test
  void markFixed_blankAction_throws400() {
    AiErrorDetail open = service.record("ACT", "desc", null, null, null, null);
    assertThatThrownBy(() -> service.markFixed(open.getId(), " "))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  void markFixed_alreadyFixed_throws409() {
    AiErrorDetail open = service.record("ACT", "desc", null, null, null, null);
    service.markFixed(open.getId(), "fix1");
    assertThatThrownBy(() -> service.markFixed(open.getId(), "fix2"))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void markFixed_unknownId_throws404() {
    assertThatThrownBy(() -> service.markFixed(999999L, "fix"))
        .isInstanceOf(NotFoundException.class);
  }

  @Test
  void list_filtersByStatus() {
    service.record("A", "d1", null, null, null, null);
    AiErrorDetail b = service.record("B", "d2", null, null, null, null);
    service.markFixed(b.getId(), "fixed");
    service.record("C", "d3", null, null, null, null);

    PageParams pp = new PageParams();
    PageResponse<AiErrorDetail> open = service.list(AiErrorStatus.OPEN, pp);
    assertThat(open.getTotal()).isEqualTo(2);
    assertThat(open.getContent()).allMatch(d -> AiErrorStatus.OPEN.equals(d.getStatus()));

    PageResponse<AiErrorDetail> fixed = service.list(AiErrorStatus.FIXED, pp);
    assertThat(fixed.getTotal()).isEqualTo(1);

    PageResponse<AiErrorDetail> all = service.list(null, pp);
    assertThat(all.getTotal()).isEqualTo(3);
  }

  @Test
  void list_invalidStatus_throws400() {
    assertThatThrownBy(() -> service.list("MAYBE", new PageParams()))
        .isInstanceOf(BadRequestException.class);
  }
}
