/* (C) 2026 Rainier — internal use only. */
package com.rainier.role.service;

import com.rainier.common.exception.ConflictException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.role.domain.Role;
import com.rainier.role.dto.RoleCreateRequest;
import com.rainier.role.dto.RoleDetail;
import com.rainier.role.dto.RoleUpdateRequest;
import com.rainier.role.repository.RoleRepository;
import com.rainier.userrole.repository.UserRoleRepository;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business operations for {@link Role}. */
@Service
@Transactional(readOnly = true)
public class RoleService {

  private final RoleRepository repo;
  private final UserRoleRepository userRoleRepo;

  public RoleService(RoleRepository repo, UserRoleRepository userRoleRepo) {
    this.repo = repo;
    this.userRoleRepo = userRoleRepo;
  }

  @Transactional
  public RoleDetail create(RoleCreateRequest req) {
    if (repo.existsByCode(req.getCode())) {
      throw new ConflictException("code already exists: " + req.getCode());
    }
    Role r = new Role();
    r.setCode(req.getCode());
    r.setName(req.getName());
    r.setDescription(req.getDescription());
    r.setEnabled(req.getEnabled() == null ? Boolean.TRUE : req.getEnabled());
    r.setAdminAccess(req.getAdminAccess() == null ? Boolean.FALSE : req.getAdminAccess());
    return RoleDetail.from(repo.saveAndFlush(r));
  }

  public RoleDetail findById(Long id) {
    return RoleDetail.from(getOrThrow(id));
  }

  public PageResponse<RoleDetail> list(Boolean enabled, PageParams page) {
    Specification<Role> spec =
        (root, query, cb) -> {
          javax.persistence.criteria.Predicate p = cb.conjunction();
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
    Page<Role> result = repo.findAll(spec, req);
    return PageResponse.of(
        result.stream().map(RoleDetail::from).collect(Collectors.toList()),
        page.getPage(),
        page.getSize(),
        result.getTotalElements());
  }

  @Transactional
  public RoleDetail update(Long id, RoleUpdateRequest req) {
    Role r = getOrThrow(id);
    r.setName(req.getName());
    if (req.getDescription() != null) {
      r.setDescription(req.getDescription());
    }
    if (req.getEnabled() != null) {
      r.setEnabled(req.getEnabled());
    }
    if (req.getAdminAccess() != null) {
      r.setAdminAccess(req.getAdminAccess());
    }
    return RoleDetail.from(repo.saveAndFlush(r));
  }

  @Transactional
  public void delete(Long id) {
    Role r = getOrThrow(id);
    long count = userRoleRepo.countByRoleId(id);
    if (count > 0) {
      throw new ConflictException("role has assignments");
    }
    repo.delete(r);
  }

  Role getOrThrow(Long id) {
    return repo.findById(id).orElseThrow(() -> new NotFoundException("role not found: id=" + id));
  }
}
