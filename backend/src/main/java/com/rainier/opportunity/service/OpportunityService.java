/* (C) 2026 Rainier — internal use only. */
package com.rainier.opportunity.service;

import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.ConflictException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.opportunity.domain.GateDecision;
import com.rainier.opportunity.domain.Opportunity;
import com.rainier.opportunity.domain.OpportunityStage;
import com.rainier.opportunity.domain.OpportunityStatus;
import com.rainier.opportunity.dto.OpportunityCreateRequest;
import com.rainier.opportunity.dto.OpportunityDetail;
import com.rainier.opportunity.dto.OpportunityUpdateRequest;
import com.rainier.opportunity.repository.OpportunityRepository;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pre-sales opportunity pipeline (v0.0.44). Stage machine LEAD→CONTRACT gated at 商机/投标/合同; WON at
 * 合同签订, LOST on any gate REJECT; 立项 (initiate) links the delivery Project once WON.
 */
@Service
@Transactional(readOnly = true)
public class OpportunityService {

  private final OpportunityRepository repo;
  private final UserRepository userRepo;
  private final ProjectRepository projectRepo;

  public OpportunityService(
      OpportunityRepository repo, UserRepository userRepo, ProjectRepository projectRepo) {
    this.repo = repo;
    this.userRepo = userRepo;
    this.projectRepo = projectRepo;
  }

  @Transactional
  public OpportunityDetail create(OpportunityCreateRequest req) {
    validateOwners(
        req.getCommercialOwnerUserId(),
        req.getSolutionOwnerUserId(),
        req.getPmUserId(),
        req.getOpsOwnerUserId());
    Opportunity o = new Opportunity();
    o.setCustomerName(req.getCustomerName());
    o.setTitle(req.getTitle());
    o.setAmount(req.getAmount());
    o.setStage(OpportunityStage.LEAD);
    o.setStatus(OpportunityStatus.OPEN);
    o.setCommercialOwnerUserId(req.getCommercialOwnerUserId());
    o.setSolutionOwnerUserId(req.getSolutionOwnerUserId());
    o.setPmUserId(req.getPmUserId());
    o.setOpsOwnerUserId(req.getOpsOwnerUserId());
    return enrich(repo.saveAndFlush(o));
  }

  public OpportunityDetail findById(Long id) {
    return enrich(getOrThrow(id));
  }

  public PageResponse<OpportunityDetail> list(
      String stage, String status, Long ownerUserId, PageParams page) {
    Specification<Opportunity> spec =
        (root, q, cb) -> {
          Predicate p = cb.conjunction();
          if (stage != null) {
            p = cb.and(p, cb.equal(root.get("stage"), stage));
          }
          if (status != null) {
            p = cb.and(p, cb.equal(root.get("status"), status));
          }
          if (ownerUserId != null) {
            p =
                cb.and(
                    p,
                    cb.or(
                        cb.equal(root.get("commercialOwnerUserId"), ownerUserId),
                        cb.equal(root.get("solutionOwnerUserId"), ownerUserId),
                        cb.equal(root.get("pmUserId"), ownerUserId),
                        cb.equal(root.get("opsOwnerUserId"), ownerUserId)));
          }
          return p;
        };
    PageRequest pr =
        PageRequest.of(
            page.getPage(),
            page.getSize(),
            Sort.by(Sort.Direction.DESC, "createTime").and(Sort.by(Sort.Direction.DESC, "id")));
    Page<Opportunity> result = repo.findAll(spec, pr);
    return PageResponse.of(
        result.stream().map(this::enrich).collect(Collectors.toList()),
        page.getPage(),
        page.getSize(),
        result.getTotalElements());
  }

  @Transactional
  public OpportunityDetail update(Long id, OpportunityUpdateRequest req) {
    Opportunity o = getOrThrow(id);
    validateOwners(
        req.getCommercialOwnerUserId(),
        req.getSolutionOwnerUserId(),
        req.getPmUserId(),
        req.getOpsOwnerUserId());
    o.setCustomerName(req.getCustomerName());
    o.setTitle(req.getTitle());
    o.setAmount(req.getAmount());
    o.setCommercialOwnerUserId(req.getCommercialOwnerUserId());
    o.setSolutionOwnerUserId(req.getSolutionOwnerUserId());
    o.setPmUserId(req.getPmUserId());
    o.setOpsOwnerUserId(req.getOpsOwnerUserId());
    return enrich(repo.saveAndFlush(o));
  }

  @Transactional
  public void delete(Long id) {
    repo.delete(getOrThrow(id));
  }

  /**
   * Advance one stage along the full 售前→实施 journey. Gate stages (商机/投标/合同/立项) require a
   * PASS/REJECT decision; a 售前 gate REJECT loses the deal (LOST), a 立项 REJECT just holds at 立项;
   * 合同签订 PASS wins the deal (WON) and enters 实施; 验收 is terminal. A LOST deal cannot advance.
   */
  @Transactional
  public OpportunityDetail advance(Long id, String decision, String note, String decidedBy) {
    Opportunity o = getOrThrow(id);
    if (OpportunityStatus.LOST.equals(o.getStatus())) {
      throw new ConflictException("opportunity is LOST");
    }
    String stage = o.getStage();
    int idx = OpportunityStage.STAGE_ORDER.indexOf(stage);
    if (idx >= OpportunityStage.STAGE_ORDER.size() - 1) {
      throw new ConflictException("opportunity already at the final stage (验收)");
    }
    if (OpportunityStage.GATE_STAGES.contains(stage)) {
      if (decision == null || !GateDecision.ALL.contains(decision)) {
        throw new BadRequestException("decision required at gate (PASS|REJECT): stage=" + stage);
      }
      o.setGateDecidedBy(decidedBy);
      if (GateDecision.REJECT.equals(decision)) {
        if (OpportunityStage.PRESALES_GATES.contains(stage)) {
          o.setStatus(OpportunityStatus.LOST); // 售前关口否决 → 丢单
        }
        // 立项评审否决 → 停在「立项」可重审，状态不变
        return enrich(repo.saveAndFlush(o));
      }
    }
    // PASS (or a non-gate stage): advance to the next node.
    o.setStage(OpportunityStage.STAGE_ORDER.get(idx + 1));
    if (OpportunityStage.CONTRACT.equals(stage)) {
      o.setStatus(OpportunityStatus.WON); // 合同签订 PASS → 赢单，进入实施
    }
    return enrich(repo.saveAndFlush(o));
  }

  /** 立项评审 gate: a WON opportunity is initiated into a delivery Project on PASS. */
  @Transactional
  public OpportunityDetail initiate(
      Long id, Long projectId, String decision, String note, String decidedBy) {
    Opportunity o = getOrThrow(id);
    if (!OpportunityStatus.WON.equals(o.getStatus())) {
      throw new ConflictException("opportunity must be WON to initiate (立项): status=" + o.getStatus());
    }
    if (decision == null || !GateDecision.ALL.contains(decision)) {
      throw new BadRequestException("decision required (PASS|REJECT)");
    }
    if (!projectRepo.existsById(projectId)) {
      throw new BadRequestException("project not found: id=" + projectId);
    }
    o.setGateDecidedBy(decidedBy);
    if (GateDecision.PASS.equals(decision)) {
      o.setProjectId(projectId); // 立项 → 链入实施 Project
    }
    return enrich(repo.saveAndFlush(o));
  }

  private void validateOwners(Long... ownerIds) {
    for (Long ownerId : ownerIds) {
      if (ownerId != null && !userRepo.existsById(ownerId)) {
        throw new BadRequestException("owner user not found: id=" + ownerId);
      }
    }
  }

  private Opportunity getOrThrow(Long id) {
    return repo
        .findById(id)
        .orElseThrow(() -> new NotFoundException("opportunity not found: id=" + id));
  }

  private OpportunityDetail enrich(Opportunity o) {
    OpportunityDetail d = OpportunityDetail.from(o);
    Set<Long> ids = new HashSet<>();
    addId(ids, o.getCommercialOwnerUserId());
    addId(ids, o.getSolutionOwnerUserId());
    addId(ids, o.getPmUserId());
    addId(ids, o.getOpsOwnerUserId());
    if (!ids.isEmpty()) {
      Map<Long, String> names = new HashMap<>();
      userRepo.findAllById(ids).forEach(u -> names.put(u.getId(), nameOf(u)));
      d.setCommercialOwnerName(names.get(o.getCommercialOwnerUserId()));
      d.setSolutionOwnerName(names.get(o.getSolutionOwnerUserId()));
      d.setPmName(names.get(o.getPmUserId()));
      d.setOpsOwnerName(names.get(o.getOpsOwnerUserId()));
    }
    return d;
  }

  private static void addId(Set<Long> ids, Long id) {
    if (id != null) {
      ids.add(id);
    }
  }

  private static String nameOf(User u) {
    return u == null ? null : u.getName();
  }
}
