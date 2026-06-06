/* (C) 2026 Rainier — internal use only. */
package com.rainier.userrole.service;

import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.ConflictException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.project.domain.Project;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.role.domain.Role;
import com.rainier.role.repository.RoleRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import com.rainier.userrole.domain.UserRole;
import com.rainier.userrole.dto.UserRoleCreateRequest;
import com.rainier.userrole.dto.UserRoleDetail;
import com.rainier.userrole.repository.UserRoleRepository;
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
 * Business operations for {@link UserRole}. Hard delete; double-defense uniqueness.
 *
 * <p>Uniqueness on {@code (user_id, role_id, project_id)} is enforced by:
 *
 * <ol>
 *   <li>DB UNIQUE constraint — covers non-NULL projectId path
 *   <li>Service-layer IS NULL query — covers NULL projectId path (MySQL UNIQUE allows multiple NULL
 *       rows)
 *   <li>{@code try/catch DataIntegrityViolationException} — races on either path become 409
 * </ol>
 */
@Service
@Transactional(readOnly = true)
public class UserRoleService {

  private final UserRoleRepository repo;
  private final UserRepository userRepo;
  private final RoleRepository roleRepo;
  private final ProjectRepository projectRepo;

  public UserRoleService(
      UserRoleRepository repo,
      UserRepository userRepo,
      RoleRepository roleRepo,
      ProjectRepository projectRepo) {
    this.repo = repo;
    this.userRepo = userRepo;
    this.roleRepo = roleRepo;
    this.projectRepo = projectRepo;
  }

  @Transactional
  public UserRoleDetail create(UserRoleCreateRequest req) {
    if (!userRepo.existsById(req.getUserId())) {
      throw new BadRequestException("user not found: id=" + req.getUserId());
    }
    if (!roleRepo.existsById(req.getRoleId())) {
      throw new BadRequestException("role not found: id=" + req.getRoleId());
    }
    // v0.0.8: activate projectId placeholder — validate existence when non-null. NULL is preserved
    // as "company-wide hat" semantics (see entity-user-role MODIFIED spec).
    if (req.getProjectId() != null && !projectRepo.existsById(req.getProjectId())) {
      throw new BadRequestException("project not found: id=" + req.getProjectId());
    }
    // NULL projectId branch — DB UNIQUE doesn't help here; service must enforce
    if (req.getProjectId() == null) {
      if (repo.existsByUserIdAndRoleIdAndProjectIdIsNull(req.getUserId(), req.getRoleId())) {
        throw new ConflictException("user-role already exists");
      }
    } else {
      if (repo.existsByUserIdAndRoleIdAndProjectId(
          req.getUserId(), req.getRoleId(), req.getProjectId())) {
        throw new ConflictException("user-role already exists");
      }
    }

    UserRole ur = new UserRole();
    ur.setUserId(req.getUserId());
    ur.setRoleId(req.getRoleId());
    ur.setProjectId(req.getProjectId());
    try {
      return enrich(repo.saveAndFlush(ur));
    } catch (DataIntegrityViolationException e) {
      // Race: existsBy* returned false but DB unique constraint rejected the row.
      throw new ConflictException("user-role already exists");
    }
  }

  public UserRoleDetail findById(Long id) {
    return enrich(getOrThrow(id));
  }

  public PageResponse<UserRoleDetail> list(
      Long userId, Long roleId, Long projectId, PageParams page) {
    Specification<UserRole> spec =
        (root, query, cb) -> {
          javax.persistence.criteria.Predicate p = cb.conjunction();
          if (userId != null) {
            p = cb.and(p, cb.equal(root.get("userId"), userId));
          }
          if (roleId != null) {
            p = cb.and(p, cb.equal(root.get("roleId"), roleId));
          }
          if (projectId != null) {
            p = cb.and(p, cb.equal(root.get("projectId"), projectId));
          }
          return p;
        };
    PageRequest req =
        PageRequest.of(page.getPage(), page.getSize(), Sort.by(Sort.Direction.DESC, "createTime"));
    Page<UserRole> result = repo.findAll(spec, req);
    List<UserRoleDetail> enriched = result.stream().map(this::enrich).collect(Collectors.toList());
    return PageResponse.of(enriched, page.getPage(), page.getSize(), result.getTotalElements());
  }

  @Transactional
  public void delete(Long id) {
    UserRole ur = getOrThrow(id);
    repo.delete(ur);
  }

  UserRole getOrThrow(Long id) {
    return repo.findById(id)
        .orElseThrow(() -> new NotFoundException("user-role not found: id=" + id));
  }

  private UserRoleDetail enrich(UserRole ur) {
    UserRoleDetail dto = UserRoleDetail.from(ur);
    User u = userRepo.findById(ur.getUserId()).orElse(null);
    if (u != null) {
      dto.setUserName(u.getName());
      dto.setUserLoginName(u.getLoginName());
    }
    Role r = roleRepo.findById(ur.getRoleId()).orElse(null);
    if (r != null) {
      dto.setRoleName(r.getName());
      dto.setRoleCode(r.getCode());
    }
    // v0.0.8: enrich project. Reads are strict (DanglingProjectIdCleanup runs at startup) but a
    // defensive null-on-miss keeps the response shape consistent if a Project deletion races a
    // concurrent read.
    if (ur.getProjectId() != null) {
      Project p = projectRepo.findById(ur.getProjectId()).orElse(null);
      if (p != null) {
        dto.setProjectName(p.getName());
        dto.setProjectCode(p.getCode());
      }
    }
    return dto;
  }
}
