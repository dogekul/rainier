/* (C) 2026 Rainier — internal use only. */
package com.rainier.user.service;

import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.ConflictException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.position.domain.Position;
import com.rainier.position.repository.PositionRepository;
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

/**
 * Business operations for {@link User}.
 *
 * <p>v0.0.7 enrichment: when reading, if {@code positionId != null} the service looks up the
 * Position and copies {@code positionName} / {@code positionCategory} into the response DTO.
 */
@Service
@Transactional(readOnly = true)
public class UserService {

  private final UserRepository repo;
  private final UserOrganizationRepository userOrgRepo;
  private final PositionRepository positionRepo;

  public UserService(
      UserRepository repo,
      UserOrganizationRepository userOrgRepo,
      PositionRepository positionRepo) {
    this.repo = repo;
    this.userOrgRepo = userOrgRepo;
    this.positionRepo = positionRepo;
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
    if (req.getPositionId() != null && !positionRepo.existsById(req.getPositionId())) {
      throw new BadRequestException("position not found: id=" + req.getPositionId());
    }
    User u = new User();
    u.setLoginName(req.getLoginName());
    u.setName(req.getName());
    u.setCode(nullIfBlank(req.getCode()));
    u.setEmailAddress(nullIfBlank(req.getEmailAddress()));
    u.setIsInternal(req.getIsInternal() == null ? Boolean.TRUE : req.getIsInternal());
    u.setEnabled(req.getEnabled() == null ? Boolean.TRUE : req.getEnabled());
    u.setPositionId(req.getPositionId());
    return enrich(repo.saveAndFlush(u));
  }

  public UserDetail findById(Long id) {
    return enrich(getOrThrow(id));
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
        result.stream().map(this::enrich).collect(Collectors.toList()),
        page.getPage(),
        page.getSize(),
        result.getTotalElements());
  }

  @Transactional
  public UserDetail update(Long id, UserUpdateRequest req) {
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
    if (req.getPositionId() != null && !positionRepo.existsById(req.getPositionId())) {
      throw new BadRequestException("position not found: id=" + req.getPositionId());
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
    // v0.0.7: positionId — UpdateRequest treats absent and explicit-null the same way (set value or
    // clear).
    u.setPositionId(req.getPositionId());
    return enrich(repo.saveAndFlush(u));
  }

  @Transactional
  public void delete(Long id) {
    User u = getOrThrow(id);
    long activeAssignments = userOrgRepo.countByUserIdAndLeftAtIsNull(id);
    if (activeAssignments > 0) {
      throw new ConflictException("has active organization assignments");
    }
    repo.delete(u);
  }

  User getOrThrow(Long id) {
    return repo.findById(id).orElseThrow(() -> new NotFoundException("user not found: id=" + id));
  }

  private UserDetail enrich(User u) {
    UserDetail dto = UserDetail.from(u);
    if (u.getPositionId() != null) {
      Position p = positionRepo.findById(u.getPositionId()).orElse(null);
      if (p != null) {
        dto.setPositionName(p.getName());
        dto.setPositionCategory(p.getCategory());
      }
    }
    return dto;
  }

  private static boolean nonBlank(String s) {
    return s != null && !s.isEmpty();
  }

  private static String nullIfBlank(String s) {
    return s == null || s.isEmpty() ? null : s;
  }
}
