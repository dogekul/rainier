/* (C) 2026 Rainier — internal use only. */
package com.rainier.me.service;

import com.rainier.me.dto.ProfileResponse;
import com.rainier.organization.domain.Organization;
import com.rainier.organization.repository.OrganizationRepository;
import com.rainier.position.repository.PositionRepository;
import com.rainier.story.repository.StoryRepository;
import com.rainier.task.repository.TaskRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import com.rainier.userorganization.domain.UserOrgRole;
import com.rainier.userorganization.domain.UserOrganization;
import com.rainier.userorganization.repository.UserOrganizationRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The「我的档案」read-model (v0.0.40): the caller's org identity + position + direct manager +
 * contribution counts. Self-scoped, all-users (token-gated). Pure aggregation — zero writes. Degrades
 * to a login-only profile when the token subject has no matching User (mirrors MeService).
 */
@Service
@Transactional(readOnly = true)
public class MeProfileService {

  /** Bound the up-the-tree manager walk (defends against an accidental parentId cycle). */
  private static final int MAX_WALK_DEPTH = 8;

  private final UserRepository userRepo;
  private final UserOrganizationRepository userOrgRepo;
  private final OrganizationRepository orgRepo;
  private final PositionRepository positionRepo;
  private final StoryRepository storyRepo;
  private final TaskRepository taskRepo;

  public MeProfileService(
      UserRepository userRepo,
      UserOrganizationRepository userOrgRepo,
      OrganizationRepository orgRepo,
      PositionRepository positionRepo,
      StoryRepository storyRepo,
      TaskRepository taskRepo) {
    this.userRepo = userRepo;
    this.userOrgRepo = userOrgRepo;
    this.orgRepo = orgRepo;
    this.positionRepo = positionRepo;
    this.storyRepo = storyRepo;
    this.taskRepo = taskRepo;
  }

  public ProfileResponse profileOf(String username) {
    ProfileResponse out = new ProfileResponse();
    out.setLoginName(username);
    User me = userRepo.findByLoginName(username).orElse(null);
    if (me == null) {
      return out; // degraded: loginName only, empty memberships, null manager, 0 counts
    }
    out.setUserId(me.getId());
    out.setName(me.getName());
    if (me.getPositionId() != null) {
      positionRepo
          .findById(me.getPositionId())
          .ifPresent(
              p -> {
                out.setPositionName(p.getName());
                out.setPositionCategory(p.getCategory());
              });
    }

    List<UserOrganization> active = userOrgRepo.findByUserIdAndLeftAtIsNull(me.getId());
    List<Long> orgIds =
        active.stream()
            .map(UserOrganization::getOrganizationId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    Map<Long, Organization> orgMap = new HashMap<>();
    orgRepo.findAllById(orgIds).forEach(o -> orgMap.put(o.getId(), o));

    List<ProfileResponse.Membership> memberships = new ArrayList<>();
    Long primaryOrgId = null;
    for (UserOrganization uo : active) {
      Organization o = orgMap.get(uo.getOrganizationId());
      if (o == null) {
        continue; // org soft-deleted (@Where) — drop the dangling membership
      }
      boolean primary = Boolean.TRUE.equals(uo.getIsPrimary());
      memberships.add(
          new ProfileResponse.Membership(
              o.getId(),
              o.getName(),
              o.getType() == null ? null : o.getType().name(),
              uo.getRole() == null ? null : uo.getRole().name(),
              primary));
      if (primary && primaryOrgId == null) {
        primaryOrgId = o.getId();
      }
    }
    out.setMemberships(memberships);

    // Manager: walk up from the primary org (fall back to the first membership if none is primary).
    Long startOrgId = primaryOrgId;
    if (startOrgId == null && !memberships.isEmpty()) {
      startOrgId = memberships.get(0).getOrganizationId();
    }
    out.setManager(resolveManager(me.getId(), startOrgId));

    out.setOwnedStoryCount(storyRepo.countByOwnerUserId(me.getId()));
    out.setAssignedTaskCount(taskRepo.countByAssigneeUserId(me.getId()));
    return out;
  }

  /** First active HEAD that is not the user, walking up the org tree from {@code startOrgId}. */
  private ProfileResponse.Manager resolveManager(Long meId, Long startOrgId) {
    Long orgId = startOrgId;
    int depth = 0;
    while (orgId != null && depth < MAX_WALK_DEPTH) {
      for (UserOrganization uo : userOrgRepo.findByOrganizationIdAndLeftAtIsNull(orgId)) {
        if (uo.getRole() == UserOrgRole.HEAD && !meId.equals(uo.getUserId())) {
          User mgr = userRepo.findById(uo.getUserId()).orElse(null);
          if (mgr != null) {
            return new ProfileResponse.Manager(mgr.getId(), mgr.getName(), mgr.getLoginName());
          }
        }
      }
      Organization o = orgRepo.findById(orgId).orElse(null);
      orgId = (o == null) ? null : o.getParentId();
      depth++;
    }
    return null;
  }
}
