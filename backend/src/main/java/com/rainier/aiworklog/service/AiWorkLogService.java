/* (C) 2026 Rainier — internal use only. */
package com.rainier.aiworklog.service;

import com.rainier.ai.service.AiErrorService;
import com.rainier.aiworklog.domain.AiWorkLog;
import com.rainier.aiworklog.domain.AiWorkLogStatus;
import com.rainier.aiworklog.dto.AiWorkLogCreateRequest;
import com.rainier.aiworklog.dto.AiWorkLogDetail;
import com.rainier.aiworklog.executor.DecisionExecutor;
import com.rainier.aiworklog.executor.ExecutorResult;
import com.rainier.aiworklog.repository.AiWorkLogRepository;
import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.ConflictException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business operations for {@link AiWorkLog} (v0.0.43, flywheel base). A proposal is created PROPOSED
 * (with evidence) and decided exactly once (PROPOSED → ACCEPTED/REJECTED). Re-deciding is rejected.
 *
 * <p>F1 (v0.0.100): on ACCEPTED, the matching {@link DecisionExecutor} actually mutates the target
 * entity and its pre-mutation snapshot is recorded so the change can be undone via {@link
 * #reverse}. REJECTED still only flips status. Executor failures are logged but do NOT block the
 * status change — reverseSnapshot just stays null and the log is non-reversible.
 */
@Service
@Transactional(readOnly = true)
public class AiWorkLogService {

  private static final Logger LOG = LoggerFactory.getLogger(AiWorkLogService.class);

  private final AiWorkLogRepository repo;
  private final List<DecisionExecutor> executors;
  private final AiErrorService aiErrorService;

  public AiWorkLogService(
      AiWorkLogRepository repo,
      List<DecisionExecutor> executors,
      AiErrorService aiErrorService) {
    this.repo = repo;
    this.executors = executors == null ? Collections.<DecisionExecutor>emptyList() : executors;
    this.aiErrorService = aiErrorService;
  }

  @Transactional
  public AiWorkLogDetail create(AiWorkLogCreateRequest req) {
    AiWorkLog a = new AiWorkLog();
    a.setAgentType(req.getAgentType());
    a.setAction(req.getAction());
    a.setTargetType(req.getTargetType());
    a.setTargetId(req.getTargetId());
    a.setSummary(req.getSummary());
    a.setEvidence(req.getEvidence());
    a.setStatus(AiWorkLogStatus.PROPOSED);
    return AiWorkLogDetail.from(repo.saveAndFlush(a));
  }

  public PageResponse<AiWorkLogDetail> query(String agentType, String status, PageParams page) {
    Specification<AiWorkLog> spec =
        (root, q, cb) -> {
          Predicate p = cb.conjunction();
          if (agentType != null) {
            p = cb.and(p, cb.equal(root.get("agentType"), agentType));
          }
          if (status != null) {
            p = cb.and(p, cb.equal(root.get("status"), status));
          }
          return p;
        };
    PageRequest pr =
        PageRequest.of(
            page.getPage(),
            page.getSize(),
            Sort.by(Sort.Direction.DESC, "createTime").and(Sort.by(Sort.Direction.DESC, "id")));
    Page<AiWorkLog> result = repo.findAll(spec, pr);
    return PageResponse.of(
        result.stream().map(AiWorkLogDetail::from).collect(Collectors.toList()),
        page.getPage(),
        page.getSize(),
        result.getTotalElements());
  }

  public AiWorkLogDetail findById(Long id) {
    return AiWorkLogDetail.from(getOrThrow(id));
  }

  /**
   * Record a human decision. Only a PROPOSED log can be decided; REJECTED requires a reason. On
   * ACCEPTED, the matching {@link DecisionExecutor} runs and its snapshot is persisted.
   */
  @Transactional
  public AiWorkLogDetail decide(Long id, String decision, String reason, String decidedBy) {
    if (decision == null || !AiWorkLogStatus.DECISIONS.contains(decision)) {
      throw new BadRequestException("invalid decision: " + decision);
    }
    AiWorkLog a = getOrThrow(id);
    if (!AiWorkLogStatus.PROPOSED.equals(a.getStatus())) {
      throw new ConflictException("already decided: " + a.getStatus());
    }
    boolean rejected = AiWorkLogStatus.REJECTED.equals(decision);
    if (rejected && (reason == null || reason.trim().isEmpty())) {
      throw new BadRequestException("reject reason is required");
    }
    a.setStatus(decision);
    a.setDecidedBy(decidedBy);
    a.setDecidedAt(Instant.now());
    a.setRejectReason(rejected ? reason : null);

    if (AiWorkLogStatus.ACCEPTED.equals(decision)) {
      DecisionExecutor executor = findExecutor(a);
      if (executor != null) {
        try {
          ExecutorResult result = executor.execute(a);
          if (result != null && result.isExecuted()) {
            a.setReverseSnapshot(result.getSnapshot());
          }
        } catch (RuntimeException ex) {
          // Decision integrity > side-effect: do not block status flip on executor failure;
          // the log just stays non-reversible (reverseSnapshot=null).
          LOG.warn(
              "AiWorkLog#{} executor {} failed: {}",
              a.getId(),
              executor.getClass().getSimpleName(),
              ex.getMessage());
          a.setReverseSnapshot(null);
        }
      }
    }
    return AiWorkLogDetail.from(repo.saveAndFlush(a));
  }

  /**
   * F1 (v0.0.100): undo a previously ACCEPTED log. Requires {@code status=ACCEPTED} and a non-empty
   * {@code reverseSnapshot}; on success the entity is restored, the log flips back to PROPOSED, and
   * a public {@code AiError(OPEN)} is automatically recorded (飞轮反向证据).
   */
  @Transactional
  public AiWorkLogDetail reverse(Long id, String reversedBy) {
    AiWorkLog a = getOrThrow(id);
    if (!AiWorkLogStatus.ACCEPTED.equals(a.getStatus())) {
      throw new BadRequestException("only ACCEPTED logs can be reversed; status=" + a.getStatus());
    }
    String snapshot = a.getReverseSnapshot();
    if (snapshot == null || snapshot.trim().isEmpty()) {
      throw new BadRequestException("no reverse snapshot available; cannot undo");
    }
    DecisionExecutor executor = findExecutor(a);
    if (executor == null) {
      throw new BadRequestException("no executor available for action=" + a.getAction());
    }
    executor.reverse(a, snapshot);

    a.setStatus(AiWorkLogStatus.PROPOSED);
    a.setReverseSnapshot(null);
    a.setReversedAt(Instant.now());
    a.setReversedBy(reversedBy);
    a.setDecidedBy(null);
    a.setDecidedAt(null);
    AiWorkLog saved = repo.saveAndFlush(a);

    // 自动公示反向证据 — KPI 信号
    try {
      aiErrorService.record(
          a.getAction(),
          "user reversed ACCEPTED ai work log #" + a.getId(),
          a.getTargetType(),
          a.getTargetId(),
          "RULE",
          snapshot);
    } catch (RuntimeException ex) {
      LOG.warn("auto-record AiError for reverse #{} failed: {}", a.getId(), ex.getMessage());
    }
    return AiWorkLogDetail.from(saved);
  }

  private DecisionExecutor findExecutor(AiWorkLog log) {
    for (DecisionExecutor e : executors) {
      if (e.supports(log)) {
        return e;
      }
    }
    return null;
  }

  private AiWorkLog getOrThrow(Long id) {
    return repo
        .findById(id)
        .orElseThrow(() -> new NotFoundException("ai work log not found: id=" + id));
  }
}
