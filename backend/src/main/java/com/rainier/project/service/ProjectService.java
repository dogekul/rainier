/* (C) 2026 Rainier — internal use only. */
package com.rainier.project.service;

import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.ConflictException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.milestone.repository.MilestoneRepository;
import com.rainier.organization.domain.Organization;
import com.rainier.organization.repository.OrganizationRepository;
import com.rainier.organizationpmo.dto.EffectivePmoDetail;
import com.rainier.organizationpmo.service.OrganizationPmoService;
import com.rainier.project.domain.Project;
import com.rainier.project.domain.ProjectStatus;
import com.rainier.project.domain.ProjectType;
import com.rainier.project.dto.ProjectCreateRequest;
import com.rainier.project.dto.ProjectDetail;
import com.rainier.project.dto.ProjectUpdateRequest;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.requirement.repository.RequirementRepository;
import com.rainier.task.repository.TaskRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import com.rainier.userorganization.domain.UserOrganization;
import com.rainier.userorganization.repository.UserOrganizationRepository;
import com.rainier.userrole.repository.UserRoleRepository;
import java.util.List;
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
  private final MilestoneRepository milestoneRepo;
  private final UserOrganizationRepository userOrgRepo;
  private final OrganizationRepository organizationRepo;
  private final OrganizationPmoService organizationPmoService;

  public ProjectService(
      ProjectRepository repo,
      UserRepository userRepo,
      RequirementRepository requirementRepo,
      UserRoleRepository userRoleRepo,
      TaskRepository taskRepo,
      MilestoneRepository milestoneRepo,
      UserOrganizationRepository userOrgRepo,
      OrganizationRepository organizationRepo,
      OrganizationPmoService organizationPmoService) {
    this.repo = repo;
    this.userRepo = userRepo;
    this.requirementRepo = requirementRepo;
    this.userRoleRepo = userRoleRepo;
    this.taskRepo = taskRepo;
    this.milestoneRepo = milestoneRepo;
    this.userOrgRepo = userOrgRepo;
    this.organizationRepo = organizationRepo;
    this.organizationPmoService = organizationPmoService;
  }

  @Transactional
  public ProjectDetail create(ProjectCreateRequest req) {
    if (!userRepo.existsById(req.getOwnerUserId())) {
      throw new BadRequestException("owner user not found: id=" + req.getOwnerUserId());
    }
    String status = req.getStatus() == null ? ProjectStatus.PLANNING : req.getStatus();
    if (!ProjectStatus.ALL.contains(status)) {
      throw new BadRequestException("invalid status: " + status);
    }
    String projectType = req.getProjectType() == null ? ProjectType.CASUAL : req.getProjectType();
    if (!ProjectType.ALL.contains(projectType)) {
      throw new BadRequestException("invalid project type: " + projectType);
    }
    // v0.0.64 — 默认值后端注入（仅在 request 缺值时）：
    // 1) 缺 organizationId → 取 owner 的主组织 (user_organization.is_primary=1 AND left_at IS NULL)
    // 2) 缺 pmoUserId 但有 organizationId → 取该组织的 effective-PMOs 首条
    Long organizationId = req.getOrganizationId();
    if (organizationId == null) {
      List<UserOrganization> primaryUo =
          userOrgRepo.findByUserIdAndIsPrimaryTrueAndLeftAtIsNull(req.getOwnerUserId());
      if (!primaryUo.isEmpty()) {
        organizationId = primaryUo.get(0).getOrganizationId();
      }
    }
    Long pmoUserId = req.getPmoUserId();
    if (pmoUserId == null && organizationId != null) {
      List<EffectivePmoDetail> effective = organizationPmoService.findEffectivePmos(organizationId);
      if (!effective.isEmpty()) {
        pmoUserId = effective.get(0).getUserId();
      }
    }

    Project p = new Project();
    // v0.0.49 — code 不再手填：先用临时占位 insert（code 列非空、无 DB UNIQUE），拿到自增 id 后回填
    // {类型前缀}-{id}。请求中的 code 一律忽略。同事务两步保存，占位 code 不外泄。
    p.setCode("__pending__");
    p.setName(req.getName());
    p.setDescription(req.getDescription());
    p.setStatus(status);
    p.setOwnerUserId(req.getOwnerUserId());
    p.setOrganizationId(organizationId);
    p.setPmoUserId(pmoUserId);
    p.setStartDate(req.getStartDate());
    p.setEndDate(req.getEndDate());
    p.setEnabled(req.getEnabled() == null ? Boolean.TRUE : req.getEnabled());
    p.setProjectType(projectType);
    Project saved = repo.saveAndFlush(p);
    saved.setCode(ProjectType.codePrefix(projectType) + "-" + saved.getId());
    return enrich(repo.saveAndFlush(saved));
  }

  public ProjectDetail findById(Long id) {
    return enrich(getOrThrow(id));
  }

  public PageResponse<ProjectDetail> list(
      String status, String projectType, Boolean enabled, PageParams page) {
    Specification<Project> spec =
        (root, query, cb) -> {
          javax.persistence.criteria.Predicate p = cb.conjunction();
          if (status != null) {
            p = cb.and(p, cb.equal(root.get("status"), status));
          }
          if (projectType != null) {
            p = cb.and(p, cb.equal(root.get("projectType"), projectType));
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
    // v0.0.16: projectType is the 轻量→正式 conversion lever — validate membership up front (mirrors
    // how status is validated early, set late). Absent/null → preserve the current value (a partial
    // payload must NOT silently downgrade a FORMAL project to CASUAL). No approval / no completeness
    // gate (A2 narrowed). The actual write happens below with the other field setters.
    if (req.getProjectType() != null && !ProjectType.ALL.contains(req.getProjectType())) {
      throw new BadRequestException("invalid project type: " + req.getProjectType());
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
    if (req.getProjectType() != null) {
      p.setProjectType(req.getProjectType());
    }
    p.setOrganizationId(req.getOrganizationId());
    // v0.0.64 — pmoUserId 可改/可清。update 不重算默认（默认仅 create 时注入）。
    p.setPmoUserId(req.getPmoUserId());
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
    // v0.0.17: cascade soft-delete this project's milestones (they have no independent meaning).
    // Reached only after the FK-protection checks pass — a 409 above rolls back with nothing
    // deleted. @SQLDelete sets del_flag=1 per row; findByProjectId returns only active rows.
    milestoneRepo.deleteAll(milestoneRepo.findByProjectId(id));
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
    if (p.getOrganizationId() != null) {
      Organization o = organizationRepo.findById(p.getOrganizationId()).orElse(null);
      if (o != null) {
        dto.setOrganizationName(o.getName());
        dto.setOrganizationType(o.getType() == null ? null : o.getType().name());
      }
    }
    if (p.getPmoUserId() != null) {
      User pmo = userRepo.findById(p.getPmoUserId()).orElse(null);
      if (pmo != null) {
        dto.setPmoName(pmo.getName());
        dto.setPmoLoginName(pmo.getLoginName());
      }
    }
    return dto;
  }
}
