/* (C) 2026 Rainier — internal use only. */
package com.rainier.userorganization.service;

import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.ConflictException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.organization.domain.Organization;
import com.rainier.organization.repository.OrganizationRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import com.rainier.userorganization.domain.UserOrgRole;
import com.rainier.userorganization.domain.UserOrganization;
import com.rainier.userorganization.dto.UserOrgCreateRequest;
import com.rainier.userorganization.dto.UserOrgDetail;
import com.rainier.userorganization.dto.UserOrgUpdateRequest;
import com.rainier.userorganization.repository.UserOrganizationRepository;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business operations for user ↔ organization assignments. */
@Service
@Transactional(readOnly = true)
public class UserOrganizationService {

  private final UserOrganizationRepository repo;
  private final UserRepository userRepo;
  private final OrganizationRepository orgRepo;

  public UserOrganizationService(
      UserOrganizationRepository repo, UserRepository userRepo, OrganizationRepository orgRepo) {
    this.repo = repo;
    this.userRepo = userRepo;
    this.orgRepo = orgRepo;
  }

  @Transactional
  public UserOrgDetail create(UserOrgCreateRequest req) {
    User user =
        userRepo
            .findById(req.getUserId())
            .orElseThrow(() -> new BadRequestException("user not found: id=" + req.getUserId()));
    Organization org =
        orgRepo
            .findById(req.getOrganizationId())
            .orElseThrow(
                () ->
                    new BadRequestException(
                        "organization not found: id=" + req.getOrganizationId()));
    if (repo.existsByUserIdAndOrganizationId(user.getId(), org.getId())) {
      throw new ConflictException("user already assigned to this organization");
    }
    UserOrganization uo = new UserOrganization();
    uo.setUserId(user.getId());
    uo.setOrganizationId(org.getId());
    uo.setRole(req.getRole() == null ? UserOrgRole.MEMBER : req.getRole());
    boolean wantsPrimary = Boolean.TRUE.equals(req.getIsPrimary());
    uo.setIsPrimary(wantsPrimary);
    uo.setJoinedAt(req.getJoinedAt() == null ? Instant.now() : req.getJoinedAt());
    uo = repo.saveAndFlush(uo);
    if (wantsPrimary) {
      repo.demoteOthersForUser(user.getId(), uo.getId());
    }
    return UserOrgDetail.from(uo, user, org);
  }

  public UserOrgDetail findById(String id) {
    UserOrganization uo = getOrThrow(id);
    User u = userRepo.findById(uo.getUserId()).orElse(null);
    Organization o = orgRepo.findById(uo.getOrganizationId()).orElse(null);
    return UserOrgDetail.from(uo, u, o);
  }

  public PageResponse<UserOrgDetail> list(
      String userId, String organizationId, UserOrgRole role, Boolean activeOnly, PageParams page) {
    Specification<UserOrganization> spec =
        (root, query, cb) -> {
          javax.persistence.criteria.Predicate p = cb.conjunction();
          if (userId != null && !userId.isEmpty()) {
            p = cb.and(p, cb.equal(root.get("userId"), userId));
          }
          if (organizationId != null && !organizationId.isEmpty()) {
            p = cb.and(p, cb.equal(root.get("organizationId"), organizationId));
          }
          if (role != null) {
            p = cb.and(p, cb.equal(root.get("role"), role));
          }
          if (Boolean.TRUE.equals(activeOnly)) {
            p = cb.and(p, cb.isNull(root.get("leftAt")));
          }
          return p;
        };
    PageRequest req =
        PageRequest.of(page.getPage(), page.getSize(), Sort.by(Sort.Direction.DESC, "createTime"));
    Page<UserOrganization> result = repo.findAll(spec, req);

    // Enrichment: batch-fetch users + organizations
    Set<String> userIds = new HashSet<>();
    Set<String> orgIds = new HashSet<>();
    for (UserOrganization uo : result) {
      userIds.add(uo.getUserId());
      orgIds.add(uo.getOrganizationId());
    }
    Map<String, User> users = byId(userRepo.findAllById(userIds));
    Map<String, Organization> orgs = byOrgId(orgRepo.findAllById(orgIds));

    List<UserOrgDetail> content =
        result.stream()
            .map(
                uo ->
                    UserOrgDetail.from(
                        uo, users.get(uo.getUserId()), orgs.get(uo.getOrganizationId())))
            .collect(Collectors.toList());
    return PageResponse.of(content, page.getPage(), page.getSize(), result.getTotalElements());
  }

  @Transactional
  public UserOrgDetail update(String id, UserOrgUpdateRequest req) {
    UserOrganization uo = getOrThrow(id);
    if (req.getRole() != null) {
      uo.setRole(req.getRole());
    }
    boolean newPrimary = Boolean.TRUE.equals(req.getIsPrimary());
    boolean wasPrimary = Boolean.TRUE.equals(uo.getIsPrimary());
    if (req.getIsPrimary() != null) {
      uo.setIsPrimary(req.getIsPrimary());
    }
    if (req.getLeftAt() != null) {
      uo.setLeftAt(req.getLeftAt());
    }
    uo = repo.saveAndFlush(uo);
    if (newPrimary && !wasPrimary) {
      repo.demoteOthersForUser(uo.getUserId(), uo.getId());
    }
    User u = userRepo.findById(uo.getUserId()).orElse(null);
    Organization o = orgRepo.findById(uo.getOrganizationId()).orElse(null);
    return UserOrgDetail.from(uo, u, o);
  }

  @Transactional
  public void delete(String id) {
    UserOrganization uo = getOrThrow(id);
    repo.delete(uo);
    repo.flush();
  }

  private UserOrganization getOrThrow(String id) {
    return repo.findById(id)
        .orElseThrow(() -> new NotFoundException("user-organization not found: id=" + id));
  }

  private static Map<String, User> byId(Collection<User> users) {
    Map<String, User> m = new HashMap<>();
    for (User u : users) {
      m.put(u.getId(), u);
    }
    return m;
  }

  private static Map<String, Organization> byOrgId(Collection<Organization> orgs) {
    Map<String, Organization> m = new HashMap<>();
    for (Organization o : orgs) {
      m.put(o.getId(), o);
    }
    return m;
  }
}
