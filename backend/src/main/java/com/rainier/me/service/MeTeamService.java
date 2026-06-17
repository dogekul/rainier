/* (C) 2026 Rainier — internal use only. */
package com.rainier.me.service;

import com.rainier.common.exception.ForbiddenException;
import com.rainier.me.dto.LedTeam;
import com.rainier.me.dto.TeamMember;
import com.rainier.organization.domain.Organization;
import com.rainier.organization.repository.OrganizationRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import com.rainier.userorganization.domain.UserOrgRole;
import com.rainier.userorganization.domain.UserOrganization;
import com.rainier.userorganization.repository.UserOrganizationRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Self-scoped team queries for the team-lead panel (v0.0.24). Identity comes from the token subject
 * (loginName) — these endpoints are token-gated, NOT admin-gated. A non-admin org HEAD can read ONLY
 * the teams they lead; reading any other team's members is refused with 403.
 */
@Service
@Transactional(readOnly = true)
public class MeTeamService {

  private final UserRepository userRepo;
  private final UserOrganizationRepository userOrgRepo;
  private final OrganizationRepository orgRepo;

  public MeTeamService(
      UserRepository userRepo,
      UserOrganizationRepository userOrgRepo,
      OrganizationRepository orgRepo) {
    this.userRepo = userRepo;
    this.userOrgRepo = userOrgRepo;
    this.orgRepo = orgRepo;
  }

  /** Teams (orgs) the caller is an active HEAD of. Empty when the subject has no matching User. */
  public List<LedTeam> ledTeams(String username) {
    User me = userRepo.findByLoginName(username).orElse(null);
    if (me == null) {
      return new ArrayList<>();
    }
    List<UserOrganization> heads =
        userOrgRepo.findByUserIdAndRoleAndLeftAtIsNull(me.getId(), UserOrgRole.HEAD);
    List<Long> orgIds =
        heads.stream().map(UserOrganization::getOrganizationId).collect(Collectors.toList());

    Map<Long, Organization> orgMap = new LinkedHashMap<>();
    orgRepo.findAllById(orgIds).forEach(o -> orgMap.put(o.getId(), o));

    List<LedTeam> result = new ArrayList<>();
    for (UserOrganization uo : heads) {
      Organization o = orgMap.get(uo.getOrganizationId());
      if (o != null) { // skip soft-deleted orgs (@Where filters them out of findAllById)
        result.add(new LedTeam(o.getId(), o.getName(), o.getType() == null ? null : o.getType().name()));
      }
    }
    return result;
  }

  /**
   * Active members of {@code organizationId}, ONLY if the caller is an active HEAD of it; otherwise
   * 403. This server-side re-check is what stops the panel from reading arbitrary teams.
   */
  public List<TeamMember> teamMembers(String username, Long organizationId) {
    User me = userRepo.findByLoginName(username).orElse(null);
    if (me == null
        || !userOrgRepo.existsByUserIdAndOrganizationIdAndRoleAndLeftAtIsNull(
            me.getId(), organizationId, UserOrgRole.HEAD)) {
      throw new ForbiddenException("not a head of organization " + organizationId);
    }
    List<UserOrganization> members = userOrgRepo.findByOrganizationIdAndLeftAtIsNull(organizationId);
    List<Long> userIds =
        members.stream()
            .map(UserOrganization::getUserId)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());

    Map<Long, User> userMap = new LinkedHashMap<>();
    userRepo.findAllById(userIds).forEach(u -> userMap.put(u.getId(), u));

    List<TeamMember> result = new ArrayList<>();
    for (Long uid : userIds) {
      User u = userMap.get(uid);
      if (u != null) {
        result.add(new TeamMember(u.getId(), u.getName(), u.getLoginName()));
      }
    }
    return result;
  }
}
