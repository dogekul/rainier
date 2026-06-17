/* (C) 2026 Rainier — internal use only. */
package com.rainier.link.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Kind of external artifact a link points at (v0.0.31). PM-system stays a 管理中心 — it only relates
 * external products (PRD/设计稿/缺陷/用例/PR live in Confluence/Figma/禅道/GitLab), never hosts them.
 */
public final class LinkType {

  public static final String PRD = "PRD";
  public static final String DESIGN = "DESIGN";
  public static final String DEFECT = "DEFECT";
  public static final String TESTCASE = "TESTCASE";
  public static final String PR = "PR";
  public static final String OTHER = "OTHER";

  public static final Set<String> ALL =
      Collections.unmodifiableSet(
          new HashSet<>(Arrays.asList(PRD, DESIGN, DEFECT, TESTCASE, PR, OTHER)));

  private LinkType() {}
}
