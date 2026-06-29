/* (C) 2026 Rainier — internal use only. */
package com.rainier.ai.service;

import com.rainier.ai.domain.AiError;
import com.rainier.ai.domain.AiErrorStatus;
import com.rainier.ai.dto.AiErrorDetail;
import com.rainier.ai.repository.AiErrorRepository;
import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.ConflictException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import javax.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business operations for {@link AiError} (v0.0.68, A4). 录入一条 OPEN 错误；markFixed 将其转为
 * FIXED，必须带 fixAction。
 */
@Service
@Transactional(readOnly = true)
public class AiErrorService {

  private final AiErrorRepository repo;

  public AiErrorService(AiErrorRepository repo) {
    this.repo = repo;
  }

  /** 录入一条 AI 错误（默认 status=OPEN, occurredAt=now 如未指定）。 */
  @Transactional
  public AiErrorDetail record(
      String aiAction,
      String errorDesc,
      String affectedEntityType,
      Long affectedEntityId,
      String rootCause,
      String evidence) {
    if (aiAction == null || aiAction.trim().isEmpty()) {
      throw new BadRequestException("aiAction is required");
    }
    if (errorDesc == null || errorDesc.trim().isEmpty()) {
      throw new BadRequestException("errorDesc is required");
    }
    AiError e = new AiError();
    e.setOccurredAt(LocalDateTime.now());
    e.setAiAction(aiAction);
    e.setErrorDesc(errorDesc);
    e.setAffectedEntityType(affectedEntityType);
    e.setAffectedEntityId(affectedEntityId);
    e.setRootCause(rootCause);
    e.setEvidence(evidence);
    e.setStatus(AiErrorStatus.OPEN);
    return AiErrorDetail.from(repo.saveAndFlush(e));
  }

  /** 标记修复：OPEN → FIXED，fixAction 必填。已是 FIXED 再次调用 → 409。 */
  @Transactional
  public AiErrorDetail markFixed(Long id, String fixAction) {
    if (fixAction == null || fixAction.trim().isEmpty()) {
      throw new BadRequestException("fixAction is required");
    }
    AiError e = getOrThrow(id);
    if (AiErrorStatus.FIXED.equals(e.getStatus())) {
      throw new ConflictException("already fixed");
    }
    e.setStatus(AiErrorStatus.FIXED);
    e.setFixAction(fixAction);
    return AiErrorDetail.from(repo.saveAndFlush(e));
  }

  public PageResponse<AiErrorDetail> list(String status, PageParams page) {
    if (status != null && !AiErrorStatus.ALL.contains(status)) {
      throw new BadRequestException("invalid status: " + status);
    }
    Specification<AiError> spec =
        (root, q, cb) -> {
          Predicate p = cb.conjunction();
          if (status != null) {
            p = cb.and(p, cb.equal(root.get("status"), status));
          }
          return p;
        };
    PageRequest pr =
        PageRequest.of(
            page.getPage(),
            page.getSize(),
            Sort.by(Sort.Direction.DESC, "occurredAt").and(Sort.by(Sort.Direction.DESC, "id")));
    Page<AiError> result = repo.findAll(spec, pr);
    return PageResponse.of(
        result.stream().map(AiErrorDetail::from).collect(Collectors.toList()),
        page.getPage(),
        page.getSize(),
        result.getTotalElements());
  }

  public AiErrorDetail findById(Long id) {
    return AiErrorDetail.from(getOrThrow(id));
  }

  /**
   * F5 (v0.0.104) — 计数 OPEN 且 occurredAt 早于 (now - thresholdHours) 的行。thresholdHours &lt;= 0
   * 视为 400 — 否则会退化成 "所有 OPEN"。
   */
  public long countOverdueOpen(int thresholdHours) {
    if (thresholdHours <= 0) {
      throw new BadRequestException("thresholdHours must be > 0");
    }
    LocalDateTime threshold = LocalDateTime.now().minusHours(thresholdHours);
    return repo.countByStatusAndOccurredAtBefore(AiErrorStatus.OPEN, threshold);
  }

  private AiError getOrThrow(Long id) {
    return repo
        .findById(id)
        .orElseThrow(() -> new NotFoundException("ai error not found: id=" + id));
  }
}
