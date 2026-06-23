/* (C) 2026 Rainier — internal use only. */
package com.rainier.opportunity.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Types of 流转产出物 (v0.0.45) attached to an opportunity at a gated transition. Extensible: add a
 * constant + label and wire it in {@link TransitionArtifactRules}. Some types are LINK-kind (a URL,
 * e.g. 讲解材料/甲方诉求清单) and some are REPORT-kind (rich text, e.g. 调研报告/纪要/得分表/差距分析).
 */
public final class ArtifactType {
  public static final String RESEARCH_REPORT = "RESEARCH_REPORT"; // 商机调研报告
  public static final String DECISION_MINUTES = "DECISION_MINUTES"; // 决策评审纪要
  // v0.0.45 POC-gate types:
  public static final String PRESENTATION_MATERIAL = "PRESENTATION_MATERIAL"; // 讲解材料 (link)
  public static final String CLIENT_REQUIREMENTS = "CLIENT_REQUIREMENTS"; // 甲方诉求清单 (link)
  public static final String POC_SCORE = "POC_SCORE"; // POC 得分表 (report)
  public static final String GAP_ANALYSIS = "GAP_ANALYSIS"; // 差距分析报告 (report)

  public static final Set<String> ALL =
      Collections.unmodifiableSet(
          new HashSet<>(
              Arrays.asList(
                  RESEARCH_REPORT,
                  DECISION_MINUTES,
                  PRESENTATION_MATERIAL,
                  CLIENT_REQUIREMENTS,
                  POC_SCORE,
                  GAP_ANALYSIS)));

  /** LINK-kind types carry a URL (the {@code link} field); others carry rich-text {@code content}. */
  public static final Set<String> LINK_TYPES =
      Collections.unmodifiableSet(
          new HashSet<>(Arrays.asList(PRESENTATION_MATERIAL, CLIENT_REQUIREMENTS)));

  private static final Map<String, String> LABELS;

  static {
    Map<String, String> m = new HashMap<>();
    m.put(RESEARCH_REPORT, "商机调研报告");
    m.put(DECISION_MINUTES, "决策评审纪要");
    m.put(PRESENTATION_MATERIAL, "讲解材料");
    m.put(CLIENT_REQUIREMENTS, "甲方诉求清单");
    m.put(POC_SCORE, "POC 得分表");
    m.put(GAP_ANALYSIS, "差距分析报告");
    LABELS = Collections.unmodifiableMap(m);
  }

  /** Chinese label for a type (falls back to the raw type if unknown). */
  public static String label(String type) {
    String l = LABELS.get(type);
    return l == null ? type : l;
  }

  public static boolean isLink(String type) {
    return LINK_TYPES.contains(type);
  }

  private ArtifactType() {}
}
