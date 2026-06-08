/* (C) 2026 Rainier — internal use only. */
package com.rainier.requirement.service;

import com.rainier.common.domain.Priority;
import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.ConflictException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.demand.repository.DemandRepository;
import com.rainier.demandrequirement.domain.DemandRequirementLink;
import com.rainier.demandrequirement.domain.LinkType;
import com.rainier.demandrequirement.repository.DemandRequirementLinkRepository;
import com.rainier.project.domain.Project;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.requirement.domain.Complexity;
import com.rainier.requirement.domain.Requirement;
import com.rainier.requirement.domain.RequirementStatus;
import com.rainier.requirement.dto.RequirementCreateRequest;
import com.rainier.requirement.dto.RequirementDetail;
import com.rainier.requirement.dto.RequirementUpdateRequest;
import com.rainier.requirement.repository.RequirementRepository;
import com.rainier.user.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business operations for {@link Requirement}.
 *
 * <p>The {@code create} path supports the workflow-demand-conversion capability: when {@code
 * sourceDemandIds} is non-empty, the requirement and N link rows are persisted in the same
 * transaction (any unknown demand id rolls back the whole operation).
 */
@Service
@Transactional(readOnly = true)
public class RequirementService {

  private final RequirementRepository repo;
  private final UserRepository userRepo;
  private final DemandRepository demandRepo;
  private final DemandRequirementLinkRepository linkRepo;
  private final ProjectRepository projectRepo;
  private final com.rainier.story.repository.StoryRepository storyRepo;

  public RequirementService(
      RequirementRepository repo,
      UserRepository userRepo,
      DemandRepository demandRepo,
      DemandRequirementLinkRepository linkRepo,
      ProjectRepository projectRepo,
      com.rainier.story.repository.StoryRepository storyRepo) {
    this.repo = repo;
    this.userRepo = userRepo;
    this.demandRepo = demandRepo;
    this.linkRepo = linkRepo;
    this.projectRepo = projectRepo;
    this.storyRepo = storyRepo;
  }

  @Transactional(rollbackFor = Exception.class)
  public RequirementDetail create(RequirementCreateRequest req) {
    if (!userRepo.existsById(req.getOwnerUserId())) {
      throw new BadRequestException("owner user not found: id=" + req.getOwnerUserId());
    }
    if (repo.existsByCode(req.getCode())) {
      throw new ConflictException("code already exists: " + req.getCode());
    }
    String status = req.getStatus() == null ? RequirementStatus.DRAFT : req.getStatus();
    if (!RequirementStatus.ALL.contains(status)) {
      throw new BadRequestException("invalid status: " + status);
    }
    String priority = req.getPriority() == null ? Priority.MEDIUM : req.getPriority();
    if (!Priority.ALL.contains(priority)) {
      throw new BadRequestException("invalid priority: " + priority);
    }
    if (req.getComplexity() != null && !Complexity.ALL.contains(req.getComplexity())) {
      throw new BadRequestException("invalid complexity: " + req.getComplexity());
    }
    // v0.0.8: activate projectId placeholder — validate existence when non-null.
    if (req.getProjectId() != null && !projectRepo.existsById(req.getProjectId())) {
      throw new BadRequestException("project not found: id=" + req.getProjectId());
    }

    Requirement r = new Requirement();
    r.setCode(req.getCode());
    r.setTitle(req.getTitle());
    r.setDescription(req.getDescription());
    r.setOwnerUserId(req.getOwnerUserId());
    r.setStatus(status);
    r.setPriority(priority);
    r.setComplexity(req.getComplexity());
    r.setProjectId(req.getProjectId());
    r.setCloseReason(req.getCloseReason());

    Requirement saved;
    try {
      saved = repo.saveAndFlush(r);
    } catch (DataIntegrityViolationException e) {
      throw new ConflictException("code already exists: " + req.getCode());
    }

    // Atomic demand-conversion (workflow-demand-conversion):
    // when sourceDemandIds is provided, validate each, create DERIVED links, rollback on any
    // unknown demand id. Dedup while preserving order so a client that sends [d1, d1, d2] only
    // produces 2 link rows instead of hitting the DB unique constraint (demand_id, requirement_id).
    List<Long> sources = req.getSourceDemandIds();
    if (sources != null && !sources.isEmpty()) {
      java.util.LinkedHashSet<Long> unique = new java.util.LinkedHashSet<>(sources);
      for (Long demandId : unique) {
        if (!demandRepo.existsById(demandId)) {
          throw new BadRequestException("demand not found: id=" + demandId);
        }
        DemandRequirementLink link = new DemandRequirementLink();
        link.setDemandId(demandId);
        link.setRequirementId(saved.getId());
        link.setLinkType(LinkType.DERIVED);
        linkRepo.save(link);
      }
      linkRepo.flush();
    }

    return enrich(saved);
  }

  public RequirementDetail findById(Long id) {
    return enrich(getOrThrow(id));
  }

  public PageResponse<RequirementDetail> list(
      String status, String priority, Long projectId, PageParams page) {
    Specification<Requirement> spec =
        (root, query, cb) -> {
          javax.persistence.criteria.Predicate p = cb.conjunction();
          if (status != null) {
            p = cb.and(p, cb.equal(root.get("status"), status));
          }
          if (priority != null) {
            p = cb.and(p, cb.equal(root.get("priority"), priority));
          }
          if (projectId != null) {
            p = cb.and(p, cb.equal(root.get("projectId"), projectId));
          }
          String search = page.getSearch();
          if (search != null && !search.isEmpty()) {
            String pattern = "%" + search.toLowerCase() + "%";
            p =
                cb.and(
                    p,
                    cb.or(
                        cb.like(cb.lower(root.get("code")), pattern),
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)));
          }
          return p;
        };
    PageRequest req =
        PageRequest.of(page.getPage(), page.getSize(), Sort.by(Sort.Direction.DESC, "createTime"));
    Page<Requirement> result = repo.findAll(spec, req);
    return PageResponse.of(
        result.stream().map(this::enrich).collect(Collectors.toList()),
        page.getPage(),
        page.getSize(),
        result.getTotalElements());
  }

  @Transactional
  public RequirementDetail update(Long id, RequirementUpdateRequest req) {
    Requirement r = getOrThrow(id);
    if (!req.getCode().equals(r.getCode())) {
      if (repo.existsByCode(req.getCode())) {
        throw new ConflictException("code already exists: " + req.getCode());
      }
      r.setCode(req.getCode());
    }
    if (req.getStatus() != null && !RequirementStatus.ALL.contains(req.getStatus())) {
      throw new BadRequestException("invalid status: " + req.getStatus());
    }
    if (req.getPriority() != null && !Priority.ALL.contains(req.getPriority())) {
      throw new BadRequestException("invalid priority: " + req.getPriority());
    }
    if (req.getComplexity() != null && !Complexity.ALL.contains(req.getComplexity())) {
      throw new BadRequestException("invalid complexity: " + req.getComplexity());
    }
    // v0.0.8: activate projectId placeholder — validate existence when non-null.
    if (req.getProjectId() != null && !projectRepo.existsById(req.getProjectId())) {
      throw new BadRequestException("project not found: id=" + req.getProjectId());
    }
    // v0.0.8: owner IS now mutable (semantic reversal of v0.0.6).
    if (!req.getOwnerUserId().equals(r.getOwnerUserId())) {
      if (!userRepo.existsById(req.getOwnerUserId())) {
        throw new BadRequestException("owner user not found: id=" + req.getOwnerUserId());
      }
      r.setOwnerUserId(req.getOwnerUserId());
    }
    r.setTitle(req.getTitle());
    r.setDescription(req.getDescription());
    if (req.getStatus() != null) {
      r.setStatus(req.getStatus());
    }
    if (req.getPriority() != null) {
      r.setPriority(req.getPriority());
    }
    if (req.getComplexity() != null) {
      r.setComplexity(req.getComplexity());
    }
    // v0.0.8: projectId may be explicitly cleared by passing null (assume PUT body is
    // fully-formed).
    r.setProjectId(req.getProjectId());
    if (req.getCloseReason() != null) {
      r.setCloseReason(req.getCloseReason());
    }
    return enrich(repo.saveAndFlush(r));
  }

  @Transactional
  public void delete(Long id) {
    Requirement r = getOrThrow(id);
    long linkCount = linkRepo.countByRequirementId(id);
    if (linkCount > 0) {
      throw new ConflictException("requirement has linked demands");
    }
    // v0.0.9: also block delete when Stories still reference this Requirement.
    long storyCount = storyRepo.countByRequirementId(id);
    if (storyCount > 0) {
      throw new ConflictException("requirement has linked stories");
    }
    repo.delete(r);
  }

  Requirement getOrThrow(Long id) {
    return repo.findById(id)
        .orElseThrow(() -> new NotFoundException("requirement not found: id=" + id));
  }

  /**
   * v0.0.8: enrich Detail with ownerName/ownerLoginName (User join) and projectName/projectCode
   * (Project join). Reads are strict (assume no dangling refs after {@code
   * DanglingProjectIdCleanup} runs at startup) but defensively returns null on miss instead of
   * throwing — keeps the response shape consistent if a referenced row is hard-deleted between the
   * cleanup pass and a concurrent read.
   */
  private RequirementDetail enrich(Requirement r) {
    RequirementDetail dto = RequirementDetail.from(r);
    userRepo
        .findById(r.getOwnerUserId())
        .ifPresent(
            u -> {
              dto.setOwnerName(u.getName());
              dto.setOwnerLoginName(u.getLoginName());
            });
    if (r.getProjectId() != null) {
      Project p = projectRepo.findById(r.getProjectId()).orElse(null);
      if (p != null) {
        dto.setProjectName(p.getName());
        dto.setProjectCode(p.getCode());
      }
    }
    // v0.0.9: storyCount enrichment for RequirementsPage drilldown count column.
    dto.setStoryCount(storyRepo.countByRequirementId(r.getId()));
    return dto;
  }
}
