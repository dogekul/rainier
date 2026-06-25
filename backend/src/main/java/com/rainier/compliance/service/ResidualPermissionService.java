/* (C) 2026 Rainier — internal use only. */
package com.rainier.compliance.service;

import com.rainier.auditlog.service.AuditLogService;
import com.rainier.auth.RequestUserContext;
import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.compliance.dto.RevokeResult;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import com.rainier.userrole.domain.UserRole;
import com.rainier.userrole.repository.UserRoleRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * v0.0.80 B7 — write-side counterpart to {@link ComplianceService#residualPermissions()}:
 * one-click cleanup of role grants held by disabled users.
 *
 * <p>Method names are deliberately NOT create/update/delete so that {@code AuditAspect} does NOT
 * auto-record on them — this service writes its own audit rows with custom action codes (
 * {@code REVOKE_RESIDUAL_ROLES}, {@code DISABLE_USER}) that carry the affected roleIds.
 */
@Service
@Transactional
public class ResidualPermissionService {

  /** v0.0.80: audit action codes. ≤16 chars to fit AuditLog.action column. */
  static final String ACTION_REVOKE = "REVOKE_RESIDUAL";

  static final String ACTION_DISABLE = "DISABLE_USER";

  static final String ENTITY_USER = "USER";

  private final UserRepository userRepo;
  private final UserRoleRepository userRoleRepo;
  private final AuditLogService auditLogService;

  public ResidualPermissionService(
      UserRepository userRepo,
      UserRoleRepository userRoleRepo,
      AuditLogService auditLogService) {
    this.userRepo = userRepo;
    this.userRoleRepo = userRoleRepo;
    this.auditLogService = auditLogService;
  }

  /**
   * {@code POST /api/compliance/users/{id}/revoke-roles} — only allowed when the user is disabled.
   * Hard-deletes every UserRole row for that userId; emits one audit row when ≥1 was removed.
   */
  public RevokeResult revokeAllRoles(Long userId) {
    User user = loadUser(userId);
    if (Boolean.TRUE.equals(user.getEnabled())) {
      throw new BadRequestException("user is still enabled; disable first before revoking roles");
    }
    int count = revokeInternal(user);
    return new RevokeResult(true, count, true);
  }

  /**
   * {@code POST /api/compliance/disable-user/{id}} — flip enabled to false (idempotent) and run the
   * same revoke-all-roles cleanup in one step.
   */
  public RevokeResult disableAndRevoke(Long userId) {
    User user = loadUser(userId);
    boolean alreadyDisabled = !Boolean.TRUE.equals(user.getEnabled());
    if (!alreadyDisabled) {
      user.setEnabled(Boolean.FALSE);
      userRepo.saveAndFlush(user);
      auditLogService.record(
          actor(), ENTITY_USER, user.getId(), ACTION_DISABLE, "DISABLE_USER USER#" + user.getId());
    }
    int count = revokeInternal(user);
    return new RevokeResult(true, count, alreadyDisabled);
  }

  /** Shared revoke step — hard-delete UserRoles, audit only when ≥1 was deleted. */
  private int revokeInternal(User user) {
    List<UserRole> grants = userRoleRepo.findByUserId(user.getId());
    if (grants.isEmpty()) {
      return 0;
    }
    List<Long> roleIds = new ArrayList<Long>();
    for (UserRole ur : grants) {
      roleIds.add(ur.getRoleId());
    }
    userRoleRepo.deleteAll(grants);
    userRoleRepo.flush();
    String summary =
        ACTION_REVOKE + " USER#" + user.getId() + " roleIds=" + roleIds.toString();
    auditLogService.record(actor(), ENTITY_USER, user.getId(), ACTION_REVOKE, summary);
    return grants.size();
  }

  private User loadUser(Long userId) {
    return userRepo
        .findById(userId)
        .orElseThrow(() -> new NotFoundException("user not found: id=" + userId));
  }

  private static String actor() {
    String a = RequestUserContext.get();
    return (a == null || a.isEmpty()) ? "system" : a;
  }
}
