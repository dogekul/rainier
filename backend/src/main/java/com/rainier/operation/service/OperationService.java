/* (C) 2026 Rainier — internal use only. */
package com.rainier.operation.service;

import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.ConflictException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.operation.domain.Operation;
import com.rainier.operation.domain.OperationStage;
import com.rainier.operation.dto.OperationCreateRequest;
import com.rainier.operation.dto.OperationDetail;
import com.rainier.operation.dto.OperationUpdateRequest;
import com.rainier.operation.repository.OperationRepository;
import com.rainier.user.repository.UserRepository;
import java.util.stream.Collectors;
import javax.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** After-sales operation pipeline (v0.0.44). Linear 回款维保→运营→复购; last advance → CLOSED. */
@Service
@Transactional(readOnly = true)
public class OperationService {

  private final OperationRepository repo;
  private final UserRepository userRepo;

  public OperationService(OperationRepository repo, UserRepository userRepo) {
    this.repo = repo;
    this.userRepo = userRepo;
  }

  @Transactional
  public OperationDetail create(OperationCreateRequest req) {
    validateOwner(req.getOpsOwnerUserId());
    Operation o = new Operation();
    o.setCustomerName(req.getCustomerName());
    o.setTitle(req.getTitle());
    o.setStage(OperationStage.MAINTENANCE);
    o.setStatus(Operation.ACTIVE);
    o.setOpsOwnerUserId(req.getOpsOwnerUserId());
    o.setProjectId(req.getProjectId());
    return enrich(repo.saveAndFlush(o));
  }

  public OperationDetail findById(Long id) {
    return enrich(getOrThrow(id));
  }

  public PageResponse<OperationDetail> list(String stage, String status, PageParams page) {
    Specification<Operation> spec =
        (root, q, cb) -> {
          Predicate p = cb.conjunction();
          if (stage != null) {
            p = cb.and(p, cb.equal(root.get("stage"), stage));
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
    Page<Operation> result = repo.findAll(spec, pr);
    return PageResponse.of(
        result.stream().map(this::enrich).collect(Collectors.toList()),
        page.getPage(),
        page.getSize(),
        result.getTotalElements());
  }

  @Transactional
  public OperationDetail update(Long id, OperationUpdateRequest req) {
    Operation o = getOrThrow(id);
    validateOwner(req.getOpsOwnerUserId());
    o.setCustomerName(req.getCustomerName());
    o.setTitle(req.getTitle());
    o.setOpsOwnerUserId(req.getOpsOwnerUserId());
    o.setProjectId(req.getProjectId());
    return enrich(repo.saveAndFlush(o));
  }

  @Transactional
  public void delete(Long id) {
    repo.delete(getOrThrow(id));
  }

  /** Advance one stage; advancing from the last stage (复购) closes the engagement. */
  @Transactional
  public OperationDetail advance(Long id) {
    Operation o = getOrThrow(id);
    if (Operation.CLOSED.equals(o.getStatus())) {
      throw new ConflictException("operation already CLOSED");
    }
    int idx = OperationStage.STAGE_ORDER.indexOf(o.getStage());
    if (idx >= OperationStage.STAGE_ORDER.size() - 1) {
      o.setStatus(Operation.CLOSED); // 复购 → 关闭
    } else {
      o.setStage(OperationStage.STAGE_ORDER.get(idx + 1));
    }
    return enrich(repo.saveAndFlush(o));
  }

  private void validateOwner(Long ownerId) {
    if (ownerId != null && !userRepo.existsById(ownerId)) {
      throw new BadRequestException("owner user not found: id=" + ownerId);
    }
  }

  private Operation getOrThrow(Long id) {
    return repo
        .findById(id)
        .orElseThrow(() -> new NotFoundException("operation not found: id=" + id));
  }

  private OperationDetail enrich(Operation o) {
    OperationDetail d = OperationDetail.from(o);
    if (o.getOpsOwnerUserId() != null) {
      userRepo.findById(o.getOpsOwnerUserId()).ifPresent(u -> d.setOpsOwnerName(u.getName()));
    }
    return d;
  }
}
