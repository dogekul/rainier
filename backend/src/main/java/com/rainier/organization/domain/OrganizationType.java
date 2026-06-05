/* (C) 2026 Rainier — internal use only. */
package com.rainier.organization.domain;

/**
 * Discriminator for the organization tree level. Mirrors the role card §2.2 hierarchy
 * (公司→部门→领域→团队→小组). Service layer recommends but does not strictly enforce parent.type → child.type
 * sequencing — small companies may skip 领域 or 小组 (角色卡片 §2.1 "角色退化").
 */
public enum OrganizationType {
  COMPANY,
  DEPARTMENT,
  DOMAIN,
  TEAM,
  SUBGROUP
}
