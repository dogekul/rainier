/* (C) 2026 Rainier — internal use only. */
package com.rainier.user.service;

import com.rainier.common.exception.ConflictException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.user.domain.User;
import com.rainier.user.dto.UserCreateRequest;
import com.rainier.user.dto.UserDetail;
import com.rainier.user.dto.UserUpdateRequest;
import com.rainier.user.repository.UserRepository;
import com.rainier.userorganization.repository.UserOrganizationRepository;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business operations for {@link User}. */
@Service
@Transactional(readOnly = true)
public class UserService {

  private final UserRepository repo;
  private final UserOrganizationRepository userOrgRepo;

  public UserService(UserRepository repo, UserOrganizationRepository userOrgRepo) {
    this.repo = repo;
    this.userOrgRepo = userOrgRepo;
  }

  @Transactional
  public UserDetail create(UserCreateRequest req) {
    if (repo.existsByLoginName(req.getLoginName())) {
      throw new ConflictException("loginName already exists: " + req.getLoginName());
    }
    if (nonBlank(req.getCode()) && repo.existsByCode(req.getCode())) {
      throw new ConflictException("code already exists: " + req.getCode());
    }
    if (nonBlank(req.getEmailAddress()) && repo.existsByEmailAddress(req.getEmailAddress())) {
      throw new ConflictException("emailAddress already exists: " + req.getEmailAddress());
    }
    User u = new User();
    u.setLoginName(req.getLoginName());
    u.setName(req.getName());
    u.setCode(nullIfBlank(req.getCode()));
    u.setEmailAddress(nullIfBlank(req.getEmailAddress()));
    u.setIsInternal(req.getIsInternal() == null ? Boolean.TRUE : req.getIsInternal());
    u.setEnabled(req.getEnabled() == null ? Boolean.TRUE : req.getEnabled());
    return UserDetail.from(repo.saveAndFlush(u));
  }

  public UserDetail findById(String id) {
    return UserDetail.from(getOrThrow(id));
  }

  public PageResponse<UserDetail> list(Boolean isInternal, Boolean enabled, PageParams page) {
    Specification<User> spec =
        (root, query, cb) -> {
          javax.persistence.criteria.Predicate p = cb.conjunction();
          if (isInternal != null) {
            p = cb.and(p, cb.equal(root.get("isInternal"), isInternal));
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
                        cb.like(cb.lower(root.get("loginName")), pattern),
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("code")), pattern),
                        cb.like(cb.lower(root.get("emailAddress")), pattern)));
          }
          return p;
        };
    PageRequest req =
        PageRequest.of(page.getPage(), page.getSize(), Sort.by(Sort.Direction.DESC, "createTime"));
    Page<User> result = repo.findAll(spec, req);
    return PageResponse.of(
        result.stream().map(UserDetail::from).collect(Collectors.toList()),
        page.getPage(),
        page.getSize(),
        result.getTotalElements());
  }

  @Transactional
  public UserDetail update(String id, UserUpdateRequest req) {
    User u = getOrThrow(id);
    if (nonBlank(req.getCode()) && !req.getCode().equals(u.getCode())) {
      if (repo.existsByCode(req.getCode())) {
        throw new ConflictException("code already exists: " + req.getCode());
      }
    }
    if (nonBlank(req.getEmailAddress()) && !req.getEmailAddress().equals(u.getEmailAddress())) {
      if (repo.existsByEmailAddress(req.getEmailAddress())) {
        throw new ConflictException("emailAddress already exists: " + req.getEmailAddress());
      }
    }
    u.setName(req.getName());
    u.setCode(nullIfBlank(req.getCode()));
    u.setEmailAddress(nullIfBlank(req.getEmailAddress()));
    if (req.getIsInternal() != null) {
      u.setIsInternal(req.getIsInternal());
    }
    if (req.getEnabled() != null) {
      u.setEnabled(req.getEnabled());
    }
    return UserDetail.from(repo.saveAndFlush(u));
  }

  @Transactional
  public void delete(String id) {
    User u = getOrThrow(id);
    long activeAssignments = userOrgRepo.countByUserIdAndLeftAtIsNull(id);
    if (activeAssignments > 0) {
      throw new ConflictException("has active organization assignments");
    }
    repo.delete(u);
  }

  User getOrThrow(String id) {
    return repo.findById(id).orElseThrow(() -> new NotFoundException("user not found: id=" + id));
  }

  private static boolean nonBlank(String s) {
    return s != null && !s.isEmpty();
  }

  private static String nullIfBlank(String s) {
    return s == null || s.isEmpty() ? null : s;
  }
}
