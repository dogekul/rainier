/* (C) 2026 Rainier — internal use only. */
package com.rainier.demand.service;

import com.rainier.common.domain.Priority;
import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.ConflictException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.demand.domain.Demand;
import com.rainier.demand.domain.DemandStatus;
import com.rainier.demand.domain.Source;
import com.rainier.demand.dto.DemandCreateRequest;
import com.rainier.demand.dto.DemandDetail;
import com.rainier.demand.dto.DemandUpdateRequest;
import com.rainier.demand.repository.DemandRepository;
import com.rainier.user.repository.UserRepository;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business operations for {@link Demand}.
 *
 * <p>FK protection ({@code DELETE} when linked) is wired in slice M04 once
 * DemandRequirementLinkRepository exists. For M02 we throw a stub message if asked to delete with
 * presumed-zero links.
 */
@Service
@Transactional(readOnly = true)
public class DemandService {

  private final DemandRepository repo;
  private final UserRepository userRepo;
  private final com.rainier.demandrequirement.repository.DemandRequirementLinkRepository linkRepo;
  private final com.rainier.opportunity.repository.OpportunityRepository opportunityRepo;

  public DemandService(
      DemandRepository repo,
      UserRepository userRepo,
      com.rainier.demandrequirement.repository.DemandRequirementLinkRepository linkRepo,
      com.rainier.opportunity.repository.OpportunityRepository opportunityRepo) {
    this.repo = repo;
    this.userRepo = userRepo;
    this.linkRepo = linkRepo;
    this.opportunityRepo = opportunityRepo;
  }

  @Transactional
  public DemandDetail create(DemandCreateRequest req) {
    if (!userRepo.existsById(req.getSubmitterUserId())) {
      throw new BadRequestException("submitter user not found: id=" + req.getSubmitterUserId());
    }
    String status = req.getStatus() == null ? DemandStatus.PENDING : req.getStatus();
    if (!DemandStatus.ALL.contains(status)) {
      throw new BadRequestException("invalid status: " + status);
    }
    String priority = req.getPriority() == null ? Priority.MEDIUM : req.getPriority();
    if (!Priority.ALL.contains(priority)) {
      throw new BadRequestException("invalid priority: " + priority);
    }
    String source = req.getSource() == null ? Source.WEB : req.getSource();
    if (!Source.ALL.contains(source)) {
      throw new BadRequestException("invalid source: " + source);
    }
    if (req.getOpportunityId() != null && !opportunityRepo.existsById(req.getOpportunityId())) {
      throw new BadRequestException("opportunity not found: id=" + req.getOpportunityId());
    }
    Demand d = new Demand();
    d.setTitle(req.getTitle());
    d.setDescription(req.getDescription());
    d.setSubmitterUserId(req.getSubmitterUserId());
    d.setStatus(status);
    d.setPriority(priority);
    d.setSource(source);
    d.setCloseReason(req.getCloseReason());
    d.setOpportunityId(req.getOpportunityId());
    return DemandDetail.from(repo.saveAndFlush(d));
  }

  public DemandDetail findById(Long id) {
    return DemandDetail.from(getOrThrow(id));
  }

  public PageResponse<DemandDetail> list(
      String status, String priority, Long opportunityId, PageParams page) {
    Specification<Demand> spec =
        (root, query, cb) -> {
          javax.persistence.criteria.Predicate p = cb.conjunction();
          if (status != null) {
            p = cb.and(p, cb.equal(root.get("status"), status));
          }
          if (priority != null) {
            p = cb.and(p, cb.equal(root.get("priority"), priority));
          }
          if (opportunityId != null) {
            p = cb.and(p, cb.equal(root.get("opportunityId"), opportunityId));
          }
          String search = page.getSearch();
          if (search != null && !search.isEmpty()) {
            String pattern = "%" + search.toLowerCase() + "%";
            p =
                cb.and(
                    p,
                    cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)));
          }
          return p;
        };
    PageRequest req =
        PageRequest.of(page.getPage(), page.getSize(), Sort.by(Sort.Direction.DESC, "createTime"));
    Page<Demand> result = repo.findAll(spec, req);
    return PageResponse.of(
        result.stream().map(DemandDetail::from).collect(Collectors.toList()),
        page.getPage(),
        page.getSize(),
        result.getTotalElements());
  }

  @Transactional
  public DemandDetail update(Long id, DemandUpdateRequest req) {
    Demand d = getOrThrow(id);
    if (req.getStatus() != null && !DemandStatus.ALL.contains(req.getStatus())) {
      throw new BadRequestException("invalid status: " + req.getStatus());
    }
    if (req.getPriority() != null && !Priority.ALL.contains(req.getPriority())) {
      throw new BadRequestException("invalid priority: " + req.getPriority());
    }
    if (req.getSource() != null && !Source.ALL.contains(req.getSource())) {
      throw new BadRequestException("invalid source: " + req.getSource());
    }
    d.setTitle(req.getTitle());
    d.setDescription(req.getDescription());
    if (req.getStatus() != null) {
      d.setStatus(req.getStatus());
    }
    if (req.getPriority() != null) {
      d.setPriority(req.getPriority());
    }
    if (req.getSource() != null) {
      d.setSource(req.getSource());
    }
    if (req.getCloseReason() != null) {
      d.setCloseReason(req.getCloseReason());
    }
    return DemandDetail.from(repo.saveAndFlush(d));
  }

  @Transactional
  public void delete(Long id) {
    Demand d = getOrThrow(id);
    long linkCount = linkRepo.countByDemandId(id);
    if (linkCount > 0) {
      throw new ConflictException("demand has linked requirements");
    }
    repo.delete(d);
  }

  Demand getOrThrow(Long id) {
    return repo.findById(id).orElseThrow(() -> new NotFoundException("demand not found: id=" + id));
  }
}
