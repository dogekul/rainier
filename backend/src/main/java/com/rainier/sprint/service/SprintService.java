/* (C) 2026 Rainier — internal use only. */
package com.rainier.sprint.service;

import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.ConflictException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.project.domain.Project;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.requirement.domain.Requirement;
import com.rainier.requirement.repository.RequirementRepository;
import com.rainier.sprint.domain.Sprint;
import com.rainier.sprint.domain.SprintStatus;
import com.rainier.sprint.dto.SprintCreateRequest;
import com.rainier.sprint.dto.SprintDetail;
import com.rainier.sprint.dto.SprintUpdateRequest;
import com.rainier.sprint.repository.SprintRepository;
import com.rainier.story.repository.StoryRepository;
import com.rainier.user.repository.UserRepository;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business operations for {@link Sprint}. v0.0.10 invariants:
 *
 * <ul>
 *   <li>{@code requirementId} immutable after create; {@code ownerUserId} mutable.
 *   <li>{@code start_date}/{@code end_date}/{@code goal} are reference metadata — service does NOT
 *       enforce time coherence (hierarchical semantics).
 *   <li>code uniqueness service-level (no DB UNIQUE).
 *   <li>Status validated against {@link SprintStatus#ALL}.
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class SprintService {

  private final SprintRepository repo;
  private final RequirementRepository requirementRepo;
  private final UserRepository userRepo;
  private final ProjectRepository projectRepo;
  private final StoryRepository storyRepo;

  public SprintService(
      SprintRepository repo,
      RequirementRepository requirementRepo,
      UserRepository userRepo,
      ProjectRepository projectRepo,
      StoryRepository storyRepo) {
    this.repo = repo;
    this.requirementRepo = requirementRepo;
    this.userRepo = userRepo;
    this.projectRepo = projectRepo;
    this.storyRepo = storyRepo;
  }

  @Transactional
  public SprintDetail create(SprintCreateRequest req) {
    if (!requirementRepo.existsById(req.getRequirementId())) {
      throw new BadRequestException("requirement not found: id=" + req.getRequirementId());
    }
    if (!userRepo.existsById(req.getOwnerUserId())) {
      throw new BadRequestException("owner user not found: id=" + req.getOwnerUserId());
    }
    if (repo.existsByCode(req.getCode())) {
      throw new ConflictException("code already exists: " + req.getCode());
    }
    String status = req.getStatus() == null ? SprintStatus.PLANNING : req.getStatus();
    if (!SprintStatus.ALL.contains(status)) {
      throw new BadRequestException("invalid status: " + status);
    }
    Sprint s = new Sprint();
    s.setCode(req.getCode());
    s.setName(req.getName());
    s.setDescription(req.getDescription());
    s.setGoal(req.getGoal());
    s.setStatus(status);
    s.setRequirementId(req.getRequirementId());
    s.setOwnerUserId(req.getOwnerUserId());
    // Sprint is hierarchical, NOT a time-box — no start ≤ end check.
    s.setStartDate(req.getStartDate());
    s.setEndDate(req.getEndDate());
    return enrich(repo.saveAndFlush(s));
  }

  public SprintDetail findById(Long id) {
    return enrich(getOrThrow(id));
  }

  public PageResponse<SprintDetail> list(Long requirementId, String status, PageParams page) {
    Specification<Sprint> spec =
        (root, query, cb) -> {
          javax.persistence.criteria.Predicate p = cb.conjunction();
          if (requirementId != null) {
            p = cb.and(p, cb.equal(root.get("requirementId"), requirementId));
          }
          if (status != null) {
            p = cb.and(p, cb.equal(root.get("status"), status));
          }
          String search = page.getSearch();
          if (search != null && !search.isEmpty()) {
            String pattern = "%" + search.toLowerCase() + "%";
            p =
                cb.and(
                    p,
                    cb.or(
                        cb.like(cb.lower(root.get("code")), pattern),
                        cb.like(cb.lower(root.get("name")), pattern)));
          }
          return p;
        };
    PageRequest pr =
        PageRequest.of(page.getPage(), page.getSize(), Sort.by(Sort.Direction.DESC, "createTime"));
    Page<Sprint> result = repo.findAll(spec, pr);
    return PageResponse.of(
        result.stream().map(this::enrich).collect(Collectors.toList()),
        page.getPage(),
        page.getSize(),
        result.getTotalElements());
  }

  @Transactional
  public SprintDetail update(Long id, SprintUpdateRequest req) {
    Sprint s = getOrThrow(id);
    if (!SprintStatus.ALL.contains(req.getStatus())) {
      throw new BadRequestException("invalid status: " + req.getStatus());
    }
    if (!req.getOwnerUserId().equals(s.getOwnerUserId())) {
      if (!userRepo.existsById(req.getOwnerUserId())) {
        throw new BadRequestException("owner user not found: id=" + req.getOwnerUserId());
      }
      s.setOwnerUserId(req.getOwnerUserId());
    }
    if (!req.getCode().equals(s.getCode())) {
      if (repo.existsByCode(req.getCode())) {
        throw new ConflictException("code already exists: " + req.getCode());
      }
      s.setCode(req.getCode());
    }
    s.setName(req.getName());
    s.setDescription(req.getDescription());
    s.setGoal(req.getGoal());
    s.setStatus(req.getStatus());
    s.setStartDate(req.getStartDate());
    s.setEndDate(req.getEndDate());
    // requirementId immutable.
    return enrich(repo.saveAndFlush(s));
  }

  @Transactional
  public void delete(Long id) {
    Sprint s = getOrThrow(id);
    long count = storyRepo.countBySprintId(id);
    if (count > 0) {
      throw new ConflictException("sprint has linked stories");
    }
    repo.delete(s);
  }

  Sprint getOrThrow(Long id) {
    return repo.findById(id).orElseThrow(() -> new NotFoundException("sprint not found: id=" + id));
  }

  /** Joins User + Requirement + Project + Story-count. Defensive null handling for read path. */
  private SprintDetail enrich(Sprint s) {
    SprintDetail dto = SprintDetail.from(s);
    userRepo
        .findById(s.getOwnerUserId())
        .ifPresent(
            u -> {
              dto.setOwnerName(u.getName());
              dto.setOwnerLoginName(u.getLoginName());
            });
    Requirement r = requirementRepo.findById(s.getRequirementId()).orElse(null);
    if (r != null) {
      dto.setRequirementCode(r.getCode());
      dto.setRequirementTitle(r.getTitle());
      // projectId inherited transitively from requirement.
      if (r.getProjectId() != null) {
        dto.setProjectId(r.getProjectId());
        Project p = projectRepo.findById(r.getProjectId()).orElse(null);
        if (p != null) {
          dto.setProjectName(p.getName());
          dto.setProjectCode(p.getCode());
        }
      }
    }
    dto.setStoryCount(storyRepo.countBySprintId(s.getId()));
    return dto;
  }
}
