/* (C) 2026 Rainier — internal use only. */
package com.rainier.userorganization.domain;

/**
 * Role a user holds within an organization. The concrete title (部门负责人 / 团队负责人 / …) is derived from
 * {@code organization.type} + {@code HEAD}; we keep just two enum values.
 */
public enum UserOrgRole {
  MEMBER,
  HEAD
}
