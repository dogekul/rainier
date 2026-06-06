/* (C) 2026 Rainier — internal use only. */
package com.rainier.demandrequirement.service;

import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.ConflictException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.demand.domain.Demand;
import com.rainier.demand.repository.DemandRepository;
import com.rainier.demandrequirement.domain.DemandRequirementLink;
import com.rainier.demandrequirement.domain.LinkType;
import com.rainier.demandrequirement.dto.DemandRequirementLinkCreateRequest;
import com.rainier.demandrequirement.dto.DemandRequirementLinkDetail;
import com.rainier.demandrequirement.dto.DerivedRequirementView;
import com.rainier.demandrequirement.dto.SourceDemandView;
import com.rainier.demandrequirement.repository.DemandRequirementLinkRepository;
import com.rainier.requirement.domain.Requirement;
import com.rainier.requirement.repository.RequirementRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business operations for {@link DemandRequirementLink}. Hard delete; no soft-delete column. */
@Service
@Transactional(readOnly = true)
public class DemandRequirementLinkService {

  private final DemandRequirementLinkRepository repo;
  private final DemandRepository demandRepo;
  private final RequirementRepository requirementRepo;

  public DemandRequirementLinkService(
      DemandRequirementLinkRepository repo,
      DemandRepository demandRepo,
      RequirementRepository requirementRepo) {
    this.repo = repo;
    this.demandRepo = demandRepo;
    this.requirementRepo = requirementRepo;
  }

  @Transactional
  public DemandRequirementLinkDetail create(DemandRequirementLinkCreateRequest req) {
    if (!demandRepo.existsById(req.getDemandId())) {
      throw new BadRequestException("demand not found: id=" + req.getDemandId());
    }
    if (!requirementRepo.existsById(req.getRequirementId())) {
      throw new BadRequestException("requirement not found: id=" + req.getRequirementId());
    }
    if (!LinkType.ALL.contains(req.getLinkType())) {
      throw new BadRequestException("invalid linkType: " + req.getLinkType());
    }
    if (repo.existsByDemandIdAndRequirementId(req.getDemandId(), req.getRequirementId())) {
      throw new ConflictException("link already exists");
    }
    DemandRequirementLink link = new DemandRequirementLink();
    link.setDemandId(req.getDemandId());
    link.setRequirementId(req.getRequirementId());
    link.setLinkType(req.getLinkType());
    try {
      return DemandRequirementLinkDetail.from(repo.saveAndFlush(link));
    } catch (DataIntegrityViolationException e) {
      // Concurrent insert race: existsByDemandIdAndRequirementId returned false but DB unique
      // constraint (demand_id, requirement_id) rejected the row. Surface as 409 not 500.
      throw new ConflictException("link already exists");
    }
  }

  public DemandRequirementLinkDetail findById(Long id) {
    return DemandRequirementLinkDetail.from(getOrThrow(id));
  }

  public PageResponse<DemandRequirementLinkDetail> list(
      Long demandId, Long requirementId, PageParams page) {
    Specification<DemandRequirementLink> spec =
        (root, query, cb) -> {
          javax.persistence.criteria.Predicate p = cb.conjunction();
          if (demandId != null) {
            p = cb.and(p, cb.equal(root.get("demandId"), demandId));
          }
          if (requirementId != null) {
            p = cb.and(p, cb.equal(root.get("requirementId"), requirementId));
          }
          return p;
        };
    PageRequest req =
        PageRequest.of(page.getPage(), page.getSize(), Sort.by(Sort.Direction.DESC, "createTime"));
    Page<DemandRequirementLink> result = repo.findAll(spec, req);
    return PageResponse.of(
        result.stream().map(DemandRequirementLinkDetail::from).collect(Collectors.toList()),
        page.getPage(),
        page.getSize(),
        result.getTotalElements());
  }

  @Transactional
  public void delete(Long id) {
    DemandRequirementLink link = getOrThrow(id);
    repo.delete(link);
  }

  /** Auxiliary query — list demands linked as sources for a requirement. */
  public List<SourceDemandView> findSourceDemands(Long requirementId) {
    if (!requirementRepo.existsById(requirementId)) {
      throw new NotFoundException("requirement not found: id=" + requirementId);
    }
    List<DemandRequirementLink> links = repo.findByRequirementId(requirementId);
    List<SourceDemandView> result = new ArrayList<>(links.size());
    for (DemandRequirementLink link : links) {
      Demand d = demandRepo.findById(link.getDemandId()).orElse(null);
      if (d != null) {
        result.add(SourceDemandView.from(d, link));
      }
    }
    return result;
  }

  /** Auxiliary query — list requirements derived from a demand. */
  public List<DerivedRequirementView> findDerivedRequirements(Long demandId) {
    if (!demandRepo.existsById(demandId)) {
      throw new NotFoundException("demand not found: id=" + demandId);
    }
    List<DemandRequirementLink> links = repo.findByDemandId(demandId);
    List<DerivedRequirementView> result = new ArrayList<>(links.size());
    for (DemandRequirementLink link : links) {
      Requirement r = requirementRepo.findById(link.getRequirementId()).orElse(null);
      if (r != null) {
        result.add(DerivedRequirementView.from(r, link));
      }
    }
    return result;
  }

  DemandRequirementLink getOrThrow(Long id) {
    return repo.findById(id).orElseThrow(() -> new NotFoundException("link not found: id=" + id));
  }
}
