/* (C) 2026 Rainier — internal use only. */
package com.rainier.compliance.service;

import com.rainier.auditlog.dto.AuditLogDetail;
import com.rainier.auditlog.repository.AuditLogRepository;
import com.rainier.compliance.dto.AuditSummary;
import com.rainier.compliance.dto.LabelCount;
import com.rainier.compliance.dto.ResidualPermission;
import com.rainier.role.domain.Role;
import com.rainier.role.repository.RoleRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import com.rainier.userrole.domain.UserRole;
import com.rainier.userrole.repository.UserRoleRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin compliance dashboard read-model (v0.0.41): audit-activity aggregation + the「停用-残留权限」
 * reconciliation. Pure read aggregation over AuditLog / User / UserRole / Role; admin-gated via
 * {@code AdminPaths} Tier A (/api/compliance).
 */
@Service
@Transactional(readOnly = true)
public class ComplianceService {

  private final AuditLogRepository auditRepo;
  private final UserRepository userRepo;
  private final UserRoleRepository userRoleRepo;
  private final RoleRepository roleRepo;

  public ComplianceService(
      AuditLogRepository auditRepo,
      UserRepository userRepo,
      UserRoleRepository userRoleRepo,
      RoleRepository roleRepo) {
    this.auditRepo = auditRepo;
    this.userRepo = userRepo;
    this.userRoleRepo = userRoleRepo;
    this.roleRepo = roleRepo;
  }

  public AuditSummary auditSummary() {
    long total = auditRepo.count();
    List<LabelCount> byAction = toLabelCounts(auditRepo.countGroupedByAction());
    List<LabelCount> byEntityType = toLabelCounts(auditRepo.countGroupedByEntityType());
    List<AuditLogDetail> recent =
        auditRepo.findTop20ByOrderByCreateTimeDescIdDesc().stream()
            .map(AuditLogDetail::from)
            .collect(Collectors.toList());
    return new AuditSummary(total, byAction, byEntityType, recent);
  }

  private static List<LabelCount> toLabelCounts(List<Object[]> rows) {
    List<LabelCount> out = new ArrayList<>();
    for (Object[] row : rows) {
      String label = row[0] == null ? null : row[0].toString();
      long count = row[1] == null ? 0L : ((Number) row[1]).longValue();
      out.add(new LabelCount(label, count));
    }
    return out;
  }

  /** Disabled users that still hold ≥1 role grant — the grants should be revoked (de-provisioning). */
  public List<ResidualPermission> residualPermissions() {
    List<ResidualPermission> out = new ArrayList<>();
    for (User u : userRepo.findByEnabledFalse()) {
      List<UserRole> grants = userRoleRepo.findByUserId(u.getId());
      if (grants.isEmpty()) {
        continue;
      }
      Set<Long> roleIds =
          grants.stream()
              .map(UserRole::getRoleId)
              .filter(Objects::nonNull)
              .collect(Collectors.toCollection(LinkedHashSet::new));
      Map<Long, Role> roleMap = new HashMap<>();
      roleRepo.findAllById(roleIds).forEach(r -> roleMap.put(r.getId(), r));
      List<String> roleNames = new ArrayList<>();
      for (Long rid : roleIds) {
        Role r = roleMap.get(rid);
        if (r != null) {
          roleNames.add(r.getName());
        }
      }
      out.add(
          new ResidualPermission(
              u.getId(), u.getName(), u.getLoginName(), grants.size(), roleNames));
    }
    return out;
  }
}
