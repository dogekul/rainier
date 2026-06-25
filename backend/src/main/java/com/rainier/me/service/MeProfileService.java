/* (C) 2026 Rainier — internal use only. */
package com.rainier.me.service;

import com.rainier.capability.dto.UserCapabilityDto;
import com.rainier.capability.service.CapabilityService;
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
 *
 * <p>v0.0.83 (C3): the aggregation core is also reused by {@code UserProfileController} for the
 * subordinate-view endpoint via {@link #profileOfUserId(Long)} and {@link #isDirectManagerOf(Long,
 * Long)} — same shape, different caller authz.
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
  private final ContributionMetricsService metricsService;
  private final CapabilityService capabilityService;

  public MeProfileService(
      UserRepository userRepo,
      UserOrganizationRepository userOrgRepo,
      OrganizationRepository orgRepo,
      PositionRepository positionRepo,
      StoryRepository storyRepo,
      TaskRepository taskRepo,
      ContributionMetricsService metricsService,
      CapabilityService capabilityService) {
    this.userRepo = userRepo;
    this.userOrgRepo = userOrgRepo;
    this.orgRepo = orgRepo;
    this.positionRepo = positionRepo;
    this.storyRepo = storyRepo;
    this.taskRepo = taskRepo;
    this.metricsService = metricsService;
    this.capabilityService = capabilityService;
  }

  public ProfileResponse profileOf(String username) {
    ProfileResponse out = new ProfileResponse();
    out.setLoginName(username);
    User me = userRepo.findByLoginName(username).orElse(null);
    if (me == null) {
      return out; // degraded: loginName only, empty memberships, null manager, 0 counts
    }
    return aggregate(me);
  }

  /**
   * v0.0.83 (C3): aggregate by user id (reused by subordinate-view endpoint). Returns null when the
   * user id does not exist — callers should map that to 404 themselves.
   */
  public ProfileResponse profileOfUserId(Long userId) {
    if (userId == null) {
      return null;
    }
    User u = userRepo.findById(userId).orElse(null);
    if (u == null) {
      return null;
    }
    return aggregate(u);
  }

  /**
   * v0.0.83 (C3): is {@code callerUserId} a direct manager of {@code targetUserId}? Direct manager =
   * caller is an active HEAD of the target's primary org (or — falling back — of that org's parent).
   * Walks AT MOST one level up; intentionally narrower than {@link #resolveManager} (which walks up
   * to {@code MAX_WALK_DEPTH}) because this is an authz gate, not a "display the manager" lookup.
   */
  public boolean isDirectManagerOf(Long callerUserId, Long targetUserId) {
    if (callerUserId == null || targetUserId == null || callerUserId.equals(targetUserId)) {
      return false;
    }
    List<UserOrganization> active = userOrgRepo.findByUserIdAndLeftAtIsNull(targetUserId);
    if (active.isEmpty()) {
      return false;
    }
    Long primaryOrgId = null;
    for (UserOrganization uo : active) {
      if (Boolean.TRUE.equals(uo.getIsPrimary()) && uo.getOrganizationId() != null) {
        primaryOrgId = uo.getOrganizationId();
        break;
      }
    }
    if (primaryOrgId == null) {
      for (UserOrganization uo : active) {
        if (uo.getOrganizationId() != null) {
          primaryOrgId = uo.getOrganizationId();
          break;
        }
      }
    }
    if (primaryOrgId == null) {
      return false;
    }
    if (isActiveHeadOfOrg(callerUserId, targetUserId, primaryOrgId)) {
      return true;
    }
    Organization primaryOrg = orgRepo.findById(primaryOrgId).orElse(null);
    Long parentOrgId = (primaryOrg == null) ? null : primaryOrg.getParentId();
    if (parentOrgId == null) {
      return false;
    }
    return isActiveHeadOfOrg(callerUserId, targetUserId, parentOrgId);
  }

  private boolean isActiveHeadOfOrg(Long callerUserId, Long targetUserId, Long orgId) {
    for (UserOrganization uo : userOrgRepo.findByOrganizationIdAndLeftAtIsNull(orgId)) {
      if (uo.getRole() == UserOrgRole.HEAD
          && uo.getUserId() != null
          && !uo.getUserId().equals(targetUserId)
          && uo.getUserId().equals(callerUserId)) {
        return true;
      }
    }
    return false;
  }

  /** Core aggregation shared by {@link #profileOf} and {@link #profileOfUserId}. */
  private ProfileResponse aggregate(User me) {
    ProfileResponse out = new ProfileResponse();
    out.setLoginName(me.getLoginName());
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
    // v0.0.84: richer contribution block (by-status / this-week / 4-week trend).
    out.setContribution(metricsService.computeFor(me.getId()));
    // v0.0.85 (C5): capability tags + level (empty list when none).
    List<UserCapabilityDto> caps = capabilityService.listUserCapabilities(me.getId());
    List<ProfileResponse.CapabilitySummary> summaries = new ArrayList<>();
    for (UserCapabilityDto c : caps) {
      summaries.add(
          new ProfileResponse.CapabilitySummary(
              c.getCapabilityTagId(),
              c.getTagName(),
              c.getTagCategory(),
              c.getLevel(),
              c.getSource()));
    }
    out.setCapabilities(summaries);
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
