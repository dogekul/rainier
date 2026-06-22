/* (C) 2026 Rainier — internal use only. */
package com.rainier.aiworklog.service;

import com.rainier.aiworklog.domain.AiWorkLog;
import com.rainier.aiworklog.domain.AiWorkLogStatus;
import com.rainier.aiworklog.dto.AiWorkLogCreateRequest;
import com.rainier.aiworklog.dto.AiWorkLogDetail;
import com.rainier.aiworklog.repository.AiWorkLogRepository;
import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.ConflictException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import java.time.Instant;
import java.util.stream.Collectors;
import javax.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business operations for {@link AiWorkLog} (v0.0.43, flywheel base). A proposal is created PROPOSED
 * (with evidence) and decided exactly once (PROPOSED → ACCEPTED/REJECTED). Re-deciding is rejected.
 */
@Service
@Transactional(readOnly = true)
public class AiWorkLogService {

  private final AiWorkLogRepository repo;

  public AiWorkLogService(AiWorkLogRepository repo) {
    this.repo = repo;
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

  /** Record a human decision. Only a PROPOSED log can be decided; REJECTED requires a reason. */
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
    return AiWorkLogDetail.from(repo.saveAndFlush(a));
  }

  private AiWorkLog getOrThrow(Long id) {
    return repo
        .findById(id)
        .orElseThrow(() -> new NotFoundException("ai work log not found: id=" + id));
  }
}
