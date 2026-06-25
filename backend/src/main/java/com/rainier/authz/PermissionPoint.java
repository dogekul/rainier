/* (C) 2026 Rainier — internal use only. */
package com.rainier.authz;

/**
 * v0.0.77 B4 — 细粒度权限点枚举。叠加在 v0.0.21 路径前缀 + 提升角色门禁之上：被
 * {@link RequiresPermission @RequiresPermission} 标注的方法只允许「持有对应 PermissionPoint」的用户调用。
 *
 * <p>新增点位时只需追加常量，不要复用现有常量含义；常量名稳定后不要重命名（数据库存的是字符串名）。
 */
public enum PermissionPoint {
  /** 维护账号（创建/启停/重置密码）— 不含权限点维护（见 ROLE_MANAGE）. */
  USER_MANAGE("维护用户账号"),
  /** 维护角色定义与角色↔权限点绑定. */
  ROLE_MANAGE("维护角色及其权限点"),
  /** 查看审计日志（鉴权事件、写操作流水）. */
  AUDIT_VIEW("查看审计日志"),
  /** 查看合规报表（鉴权统计、停用残留权限对账）. */
  COMPLIANCE_VIEW("查看合规报表"),
  /** 跨项目管理权（创建/归档项目，调整任意项目成员）. */
  PROJECT_ADMIN("跨项目管理");

  private final String description;

  PermissionPoint(String description) {
    this.description = description;
  }

  public String description() {
    return description;
  }
}
