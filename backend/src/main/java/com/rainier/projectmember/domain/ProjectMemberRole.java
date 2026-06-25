/* (C) 2026 Rainier — internal use only. */
package com.rainier.projectmember.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * v0.0.64 — 项目内角色枚举常量（与 rainier_role 解耦：本 enum 描述身份，不挂权限）。
 *
 * <ul>
 *   <li>OWNER / PMO — 合成虚拟 role；listMembers 时由 service 自动注入。<b>不</b>用于 add/update 入参。
 *   <li>PD / DEV / QA / DESIGN / BIZ / OPS / OTHER — 真实可分配 role；ALL 集合用于 service 层入参校验。
 * </ul>
 */
public final class ProjectMemberRole {

  public static final String OWNER = "OWNER";
  public static final String PMO = "PMO";

  public static final String PD = "PD";
  public static final String DEV = "DEV";
  public static final String QA = "QA";
  public static final String DESIGN = "DESIGN";
  public static final String BIZ = "BIZ";
  public static final String OPS = "OPS";
  public static final String OTHER = "OTHER";

  /** Real assignable roles — used by service-layer validation. */
  public static final Set<String> ALL =
      Collections.unmodifiableSet(
          new HashSet<String>(Arrays.asList(PD, DEV, QA, DESIGN, BIZ, OPS, OTHER)));

  private ProjectMemberRole() {}
}
