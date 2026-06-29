/* (C) 2026 Rainier — internal use only. */
package com.rainier.me.service;

import com.rainier.me.dto.Subordinate;
import com.rainier.organization.domain.Organization;
import com.rainier.organization.repository.OrganizationRepository;
import com.rainier.task.repository.TaskRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import com.rainier.userorganization.domain.UserOrgRole;
import com.rainier.userorganization.domain.UserOrganization;
import com.rainier.userorganization.repository.UserOrganizationRepository;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * v0.0.111 (H4) — read-model behind「我的下属」面板. Returns the active members of the orgs the caller
 * is an active HEAD of (one level — direct subordinates only), excluding self. Per row: identity
 * basics + a slim contribution summary (this-week DONE + total assigned tasks).
 *
 * <p>Pure read service. Non-HEAD caller → empty list (no 403; the panel itself is the gate).
 */
@Service
@Transactional(readOnly = true)
public class MeSubordinatesService {

  private final UserRepository userRepo;
  private final UserOrganizationRepository userOrgRepo;
  private final OrganizationRepository orgRepo;
  private final TaskRepository taskRepo;

  public MeSubordinatesService(
      UserRepository userRepo,
      UserOrganizationRepository userOrgRepo,
      OrganizationRepository orgRepo,
      TaskRepository taskRepo) {
    this.userRepo = userRepo;
    this.userOrgRepo = userOrgRepo;
    this.orgRepo = orgRepo;
    this.taskRepo = taskRepo;
  }

  public List<Subordinate> subordinatesOf(String username) {
    User me = userRepo.findByLoginName(username).orElse(null);
    if (me == null) {
      return new ArrayList<Subordinate>();
    }
    // Orgs the caller is an active HEAD of.
    List<UserOrganization> headRows =
        userOrgRepo.findByUserIdAndRoleAndLeftAtIsNull(me.getId(), UserOrgRole.HEAD);
    if (headRows.isEmpty()) {
      return new ArrayList<Subordinate>();
    }
    Set<Long> headOrgIds = new LinkedHashSet<Long>();
    for (UserOrganization uo : headRows) {
      if (uo.getOrganizationId() != null) {
        headOrgIds.add(uo.getOrganizationId());
      }
    }

    // Collect active member user-ids across all headed orgs (excluding self), de-duped.
    Set<Long> memberUserIds = new LinkedHashSet<Long>();
    for (Long orgId : headOrgIds) {
      for (UserOrganization uo : userOrgRepo.findByOrganizationIdAndLeftAtIsNull(orgId)) {
        Long uid = uo.getUserId();
        if (uid != null && !uid.equals(me.getId())) {
          memberUserIds.add(uid);
        }
      }
    }
    if (memberUserIds.isEmpty()) {
      return new ArrayList<Subordinate>();
    }

    // Hydrate users (drops soft-deleted via @Where) and resolve primary-org name per user.
    Map<Long, User> userMap = new HashMap<Long, User>();
    for (User u : userRepo.findAllById(memberUserIds)) {
      userMap.put(u.getId(), u);
    }
    Map<Long, String> primaryOrgNameByUser = resolvePrimaryOrgNames(memberUserIds);

    Instant weekStart = weekStartUtc(Instant.now());
    List<Subordinate> result = new ArrayList<Subordinate>();
    for (Long uid : memberUserIds) {
      User u = userMap.get(uid);
      if (u == null) {
        continue; // dangling membership: user soft-deleted
      }
      long total = taskRepo.countByAssigneeUserId(uid);
      long weeklyDone =
          taskRepo.countByAssigneeUserIdAndStatusAndUpdateTimeGreaterThanEqual(
              uid, com.rainier.task.domain.TaskStatus.DONE, weekStart);
      result.add(
          new Subordinate(
              u.getId(),
              u.getLoginName(),
              u.getName(),
              primaryOrgNameByUser.get(uid),
              new Subordinate.ContributionSummary(weeklyDone, total)));
    }
    return result;
  }

  private Map<Long, String> resolvePrimaryOrgNames(Set<Long> userIds) {
    Map<Long, Long> primaryOrgIdByUser = new HashMap<Long, Long>();
    Set<Long> orgIds = new HashSet<Long>();
    for (Long uid : userIds) {
      List<UserOrganization> primaries = userOrgRepo.findByUserIdAndIsPrimaryTrueAndLeftAtIsNull(uid);
      if (primaries.isEmpty()) {
        // Fall back to any active membership so the column is never blank when one exists.
        List<UserOrganization> active = userOrgRepo.findByUserIdAndLeftAtIsNull(uid);
        if (!active.isEmpty()) {
          Long oid = active.get(0).getOrganizationId();
          if (oid != null) {
            primaryOrgIdByUser.put(uid, oid);
            orgIds.add(oid);
          }
        }
      } else {
        Long oid = primaries.get(0).getOrganizationId();
        if (oid != null) {
          primaryOrgIdByUser.put(uid, oid);
          orgIds.add(oid);
        }
      }
    }
    Map<Long, String> orgNameById = new HashMap<Long, String>();
    if (!orgIds.isEmpty()) {
      for (Organization o : orgRepo.findAllById(orgIds)) {
        orgNameById.put(o.getId(), o.getName());
      }
    }
    Map<Long, String> out = new HashMap<Long, String>();
    for (Map.Entry<Long, Long> e : primaryOrgIdByUser.entrySet()) {
      out.put(e.getKey(), orgNameById.get(e.getValue()));
    }
    return out;
  }

  /** ISO week start (Monday 00:00 UTC) containing {@code now}. Matches ContributionMetricsService. */
  static Instant weekStartUtc(Instant now) {
    LocalDate today = LocalDateTime.ofInstant(now, ZoneOffset.UTC).toLocalDate();
    int delta = today.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue();
    LocalDate monday = today.minusDays(delta);
    return monday.atStartOfDay(ZoneOffset.UTC).toInstant();
  }
}
