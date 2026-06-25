/* (C) 2026 Rainier — internal use only. */
package com.rainier.authz.permission;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** v0.0.77 B4 — repository for {@link RolePermission}. */
@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

  List<RolePermission> findByRoleIdIn(Collection<Long> roleIds);

  List<RolePermission> findByRoleId(Long roleId);

  boolean existsByRoleIdAndPermissionPoint(Long roleId, String permissionPoint);

  long deleteByRoleIdAndPermissionPoint(Long roleId, String permissionPoint);
}
