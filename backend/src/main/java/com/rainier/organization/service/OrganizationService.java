/* (C) 2026 Rainier — internal use only. */
package com.rainier.organization.service;

import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.ConflictException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.organization.domain.Organization;
import com.rainier.organization.domain.OrganizationType;
import com.rainier.organization.dto.OrganizationCreateRequest;
import com.rainier.organization.dto.OrganizationDetail;
import com.rainier.organization.dto.OrganizationMoveRequest;
import com.rainier.organization.dto.OrganizationUpdateRequest;
import com.rainier.organization.repository.OrganizationRepository;
import com.rainier.userorganization.repository.UserOrganizationRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business operations for the organization tree.
 *
 * <p>Maintains {@code path} and {@code whole_name} caches on writes so reads are O(1). All service
 * operations consult {@code @Where("del_flag = 0")} via the repository, hiding soft-deleted rows.
 */
@Service
@Transactional(readOnly = true)
public class OrganizationService {

  private final OrganizationRepository repo;
  private final UserOrganizationRepository userOrgRepo;

  public OrganizationService(OrganizationRepository repo, UserOrganizationRepository userOrgRepo) {
    this.repo = repo;
    this.userOrgRepo = userOrgRepo;
  }

  // ---------------------------- create ----------------------------

  @Transactional
  public OrganizationDetail create(OrganizationCreateRequest req) {
    Long parentId = req.getParentId();
    String parentPath = null;
    String parentWholeName = null;
    if (parentId != null) {
      Organization parent =
          repo.findById(parentId)
              .orElseThrow(
                  () -> new BadRequestException("parent organization not found: id=" + parentId));
      parentPath = parent.getPath();
      parentWholeName = parent.getWholeName();
      if (repo.existsByParentIdAndCode(parentId, req.getCode())) {
        throw new ConflictException(
            "code already exists under the same parent: code=" + req.getCode());
      }
    } else {
      if (repo.existsByParentIdIsNullAndCode(req.getCode())) {
        throw new ConflictException("code already exists at root: code=" + req.getCode());
      }
    }

    Organization o = new Organization();
    o.setParentId(parentId);
    o.setType(req.getType());
    o.setCode(req.getCode());
    o.setName(req.getName());
    o.setDescription(req.getDescription());
    o.setEnabled(req.getEnabled() == null ? Boolean.TRUE : req.getEnabled());

    o = repo.saveAndFlush(o);
    o.setPath(parentPath == null ? "/" + o.getId() : parentPath + "/" + o.getId());
    o.setWholeName(parentWholeName == null ? o.getName() : parentWholeName + "/" + o.getName());
    o = repo.saveAndFlush(o);
    return OrganizationDetail.from(o);
  }

  // ---------------------------- read ------------------------------

  public OrganizationDetail findById(Long id) {
    return OrganizationDetail.from(getOrThrow(id));
  }

  public List<OrganizationDetail> findTree() {
    return repo.findAllByOrderByPathAsc().stream()
        .map(OrganizationDetail::from)
        .collect(Collectors.toList());
  }

  public PageResponse<OrganizationDetail> list(
      OrganizationType type, Long parentId, PageParams page) {
    Specification<Organization> spec =
        (root, query, cb) -> {
          javax.persistence.criteria.Predicate p = cb.conjunction();
          if (type != null) {
            p = cb.and(p, cb.equal(root.get("type"), type));
          }
          if (parentId != null) {
            p = cb.and(p, cb.equal(root.get("parentId"), parentId));
          }
          String search = page.getSearch();
          if (search != null && !search.isEmpty()) {
            String pattern = "%" + search.toLowerCase() + "%";
            p =
                cb.and(
                    p,
                    cb.or(
                        cb.like(cb.lower(root.get("code")), pattern),
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("wholeName")), pattern)));
          }
          return p;
        };
    PageRequest req =
        PageRequest.of(page.getPage(), page.getSize(), Sort.by(Sort.Direction.DESC, "createTime"));
    Page<Organization> result = repo.findAll(spec, req);
    return PageResponse.of(
        result.stream().map(OrganizationDetail::from).collect(Collectors.toList()),
        page.getPage(),
        page.getSize(),
        result.getTotalElements());
  }

  // ---------------------------- update ----------------------------

  @Transactional
  public OrganizationDetail update(Long id, OrganizationUpdateRequest req) {
    Organization o = getOrThrow(id);
    boolean nameChanged = !o.getName().equals(req.getName());
    String oldWholeName = o.getWholeName();

    // code change: re-check (parent_id, code) uniqueness excluding self
    if (req.getCode() != null && !req.getCode().equals(o.getCode())) {
      if (o.getParentId() == null) {
        if (repo.existsByParentIdIsNullAndCode(req.getCode())) {
          throw new ConflictException("code already exists at root: code=" + req.getCode());
        }
      } else {
        if (repo.existsByParentIdAndCode(o.getParentId(), req.getCode())) {
          throw new ConflictException(
              "code already exists under the same parent: code=" + req.getCode());
        }
      }
      o.setCode(req.getCode());
    }

    o.setName(req.getName());
    if (req.getDescription() != null) {
      o.setDescription(req.getDescription());
    }
    if (req.getEnabled() != null) {
      o.setEnabled(req.getEnabled());
    }
    String newWholeName = computeWholeName(o);
    o.setWholeName(newWholeName);
    o = repo.saveAndFlush(o);

    if (nameChanged) {
      cascadeWholeNameToDescendants(o.getPath(), oldWholeName, newWholeName);
    }
    return OrganizationDetail.from(o);
  }

  @Transactional
  public OrganizationDetail move(Long id, OrganizationMoveRequest req) {
    Organization o = getOrThrow(id);
    Long newParentId = req.getParentId();

    String newParentPath = null;
    String newParentWholeName = null;
    if (newParentId != null) {
      if (newParentId.equals(id)) {
        throw new ConflictException("cannot move organization to itself");
      }
      Organization newParent =
          repo.findById(newParentId)
              .orElseThrow(
                  () ->
                      new BadRequestException("parent organization not found: id=" + newParentId));
      // anti-cycle: new parent must not be a descendant of self
      if (newParent.getPath() != null
          && o.getPath() != null
          && (newParent.getPath().equals(o.getPath())
              || newParent.getPath().startsWith(o.getPath() + "/"))) {
        throw new ConflictException("cannot move to descendant");
      }
      newParentPath = newParent.getPath();
      newParentWholeName = newParent.getWholeName();
    }

    String oldPath = o.getPath();
    String oldWholeName = o.getWholeName();
    String newPath = newParentPath == null ? "/" + o.getId() : newParentPath + "/" + o.getId();
    String newWholeName =
        newParentWholeName == null ? o.getName() : newParentWholeName + "/" + o.getName();

    o.setParentId(newParentId);
    o.setPath(newPath);
    o.setWholeName(newWholeName);
    o = repo.saveAndFlush(o);

    cascadePathAndWholeNameToDescendants(oldPath, newPath, oldWholeName, newWholeName);
    return OrganizationDetail.from(o);
  }

  // ---------------------------- delete ----------------------------

  @Transactional
  public void delete(Long id) {
    Organization o = getOrThrow(id);
    long childCount = repo.countByParentId(id);
    if (childCount > 0) {
      throw new ConflictException("has child organizations");
    }
    long activeAssignments = userOrgRepo.countByOrganizationIdAndLeftAtIsNull(id);
    if (activeAssignments > 0) {
      throw new ConflictException("has assigned users");
    }
    repo.delete(o);
  }

  // ---------------------------- helpers ----------------------------

  Organization getOrThrow(Long id) {
    return repo.findById(id)
        .orElseThrow(() -> new NotFoundException("organization not found: id=" + id));
  }

  /**
   * v0.0.64 — 沿 parent_id 链向上回溯，返回 [self, parent, grandparent, ...]（顺序：自身 → 根）。
   *
   * <p>停止条件：parent_id == NULL 或 parent 已被删（找不到）。环路防御（虽然 DB 应不允许）：访问过的 id 集合检查。
   */
  public java.util.List<Long> getAncestorIds(Long orgId) {
    java.util.List<Long> result = new java.util.ArrayList<Long>();
    java.util.Set<Long> seen = new java.util.HashSet<Long>();
    Long cursor = orgId;
    while (cursor != null && !seen.contains(cursor)) {
      Organization o = repo.findById(cursor).orElse(null);
      if (o == null) {
        break;
      }
      result.add(cursor);
      seen.add(cursor);
      cursor = o.getParentId();
    }
    return result;
  }

  /** v0.0.64 — 按 id 查 name（OrganizationPmoService enrich 用，避开重复加载）。 */
  public java.util.Map<Long, Organization> findAllById(java.util.Collection<Long> ids) {
    java.util.Map<Long, Organization> map = new java.util.HashMap<Long, Organization>();
    for (Organization o : repo.findAllById(ids)) {
      map.put(o.getId(), o);
    }
    return map;
  }

  private String computeWholeName(Organization o) {
    if (o.getParentId() == null) {
      return o.getName();
    }
    Organization parent =
        repo.findById(o.getParentId())
            .orElseThrow(
                () ->
                    new BadRequestException(
                        "parent disappeared mid-update: id=" + o.getParentId()));
    return parent.getWholeName() + "/" + o.getName();
  }

  private void cascadeWholeNameToDescendants(
      String selfPath, String oldSelfWholeName, String newSelfWholeName) {
    if (selfPath == null || oldSelfWholeName.equals(newSelfWholeName)) {
      return;
    }
    String descendantPrefix = selfPath + "/";
    List<Organization> descendants =
        repo.findAll((root, q, cb) -> cb.like(root.get("path"), descendantPrefix + "%"));
    for (Organization d : descendants) {
      // Replace only the leading segment matching oldSelfWholeName.
      if (d.getWholeName() != null && d.getWholeName().startsWith(oldSelfWholeName + "/")) {
        d.setWholeName(newSelfWholeName + d.getWholeName().substring(oldSelfWholeName.length()));
      }
    }
    repo.saveAll(descendants);
    repo.flush();
  }

  private void cascadePathAndWholeNameToDescendants(
      String oldSelfPath, String newSelfPath, String oldSelfWholeName, String newSelfWholeName) {
    if (oldSelfPath == null || oldSelfPath.equals(newSelfPath)) {
      cascadeWholeNameToDescendants(newSelfPath, oldSelfWholeName, newSelfWholeName);
      return;
    }
    String descendantPrefix = oldSelfPath + "/";
    List<Organization> descendants =
        repo.findAll((root, q, cb) -> cb.like(root.get("path"), descendantPrefix + "%"));
    for (Organization d : descendants) {
      d.setPath(newSelfPath + d.getPath().substring(oldSelfPath.length()));
      if (d.getWholeName() != null && d.getWholeName().startsWith(oldSelfWholeName + "/")) {
        d.setWholeName(newSelfWholeName + d.getWholeName().substring(oldSelfWholeName.length()));
      }
    }
    repo.saveAll(descendants);
    repo.flush();
  }
}
