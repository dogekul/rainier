/* (C) 2026 Rainier — internal use only. */
package com.rainier.project.service;

import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.ConflictException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.project.domain.Project;
import com.rainier.project.domain.ProjectStatus;
import com.rainier.project.dto.ProjectCreateRequest;
import com.rainier.project.dto.ProjectDetail;
import com.rainier.project.dto.ProjectUpdateRequest;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.requirement.repository.RequirementRepository;
import com.rainier.task.repository.TaskRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import com.rainier.userrole.repository.UserRoleRepository;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business operations for {@link Project}.
 *
 * <p>Owner mutability: this entity differs from v0.0.6 Requirement (immutable) — v0.0.8 makes both
 * owners mutable. Service.update validates new ownerUserId exists.
 *
 * <p>FK protection on delete: a Project cannot be deleted while any Requirement or UserRole row
 * still references it via {@code project_id}.
 */
@Service
@Transactional(readOnly = true)
public class ProjectService {

  private final ProjectRepository repo;
  private final UserRepository userRepo;
  private final RequirementRepository requirementRepo;
  private final UserRoleRepository userRoleRepo;
  private final TaskRepository taskRepo;

  public ProjectService(
      ProjectRepository repo,
      UserRepository userRepo,
      RequirementRepository requirementRepo,
      UserRoleRepository userRoleRepo,
      TaskRepository taskRepo) {
    this.repo = repo;
    this.userRepo = userRepo;
    this.requirementRepo = requirementRepo;
    this.userRoleRepo = userRoleRepo;
    this.taskRepo = taskRepo;
  }

  @Transactional
  public ProjectDetail create(ProjectCreateRequest req) {
    if (!userRepo.existsById(req.getOwnerUserId())) {
      throw new BadRequestException("owner user not found: id=" + req.getOwnerUserId());
    }
    if (repo.existsByCode(req.getCode())) {
      throw new ConflictException("code already exists: " + req.getCode());
    }
    String status = req.getStatus() == null ? ProjectStatus.PLANNING : req.getStatus();
    if (!ProjectStatus.ALL.contains(status)) {
      throw new BadRequestException("invalid status: " + status);
    }
    Project p = new Project();
    p.setCode(req.getCode());
    p.setName(req.getName());
    p.setDescription(req.getDescription());
    p.setStatus(status);
    p.setOwnerUserId(req.getOwnerUserId());
    p.setStartDate(req.getStartDate());
    p.setEndDate(req.getEndDate());
    p.setEnabled(req.getEnabled() == null ? Boolean.TRUE : req.getEnabled());
    return enrich(repo.saveAndFlush(p));
  }

  public ProjectDetail findById(Long id) {
    return enrich(getOrThrow(id));
  }

  public PageResponse<ProjectDetail> list(String status, Boolean enabled, PageParams page) {
    Specification<Project> spec =
        (root, query, cb) -> {
          javax.persistence.criteria.Predicate p = cb.conjunction();
          if (status != null) {
            p = cb.and(p, cb.equal(root.get("status"), status));
          }
          if (enabled != null) {
            p = cb.and(p, cb.equal(root.get("enabled"), enabled));
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
    PageRequest req =
        PageRequest.of(page.getPage(), page.getSize(), Sort.by(Sort.Direction.DESC, "createTime"));
    Page<Project> result = repo.findAll(spec, req);
    return PageResponse.of(
        result.stream().map(this::enrich).collect(Collectors.toList()),
        page.getPage(),
        page.getSize(),
        result.getTotalElements());
  }

  @Transactional
  public ProjectDetail update(Long id, ProjectUpdateRequest req) {
    Project p = getOrThrow(id);
    if (!ProjectStatus.ALL.contains(req.getStatus())) {
      throw new BadRequestException("invalid status: " + req.getStatus());
    }
    if (!req.getOwnerUserId().equals(p.getOwnerUserId())) {
      if (!userRepo.existsById(req.getOwnerUserId())) {
        throw new BadRequestException("owner user not found: id=" + req.getOwnerUserId());
      }
      p.setOwnerUserId(req.getOwnerUserId());
    }
    p.setName(req.getName());
    // v0.0.8.1: description is now full-replace (null clears) for parity with name/status/dates.
    // Frontend ProjectsPage always sends description as a string so an empty-string clears the
    // field; omitted-as-null also clears. Resolves Code-M3 / Code-M6 (clear-description path).
    p.setDescription(req.getDescription());
    p.setStatus(req.getStatus());
    p.setStartDate(req.getStartDate());
    p.setEndDate(req.getEndDate());
    // enabled stays null-guarded — DB column is NOT NULL bit(1); silently swallowing a malformed
    // payload missing this field is safer than throwing.
    if (req.getEnabled() != null) {
      p.setEnabled(req.getEnabled());
    }
    return enrich(repo.saveAndFlush(p));
  }

  @Transactional
  public void delete(Long id) {
    Project p = getOrThrow(id);
    long reqCount = requirementRepo.countByProjectId(id);
    if (reqCount > 0) {
      throw new ConflictException("project has linked requirements");
    }
    long urCount = userRoleRepo.countByProjectId(id);
    if (urCount > 0) {
      throw new ConflictException("project has assigned user-roles");
    }
    // v0.0.11 Decision 7: append Task to FK chain. Order: Requirement → UserRole → Task.
    long taskCount = taskRepo.countByProjectId(id);
    if (taskCount > 0) {
      throw new ConflictException("project has linked tasks");
    }
    repo.delete(p);
  }

  Project getOrThrow(Long id) {
    return repo.findById(id)
        .orElseThrow(() -> new NotFoundException("project not found: id=" + id));
  }

  private ProjectDetail enrich(Project p) {
    ProjectDetail dto = ProjectDetail.from(p);
    User u = userRepo.findById(p.getOwnerUserId()).orElse(null);
    if (u != null) {
      dto.setOwnerName(u.getName());
      dto.setOwnerLoginName(u.getLoginName());
    }
    return dto;
  }
}
