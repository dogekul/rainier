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
  // v0.0.46 投标/合同-gate types (附件先 URL 占位 → 链接类，评审会议纪要为报告类):
  public static final String BID_DOCUMENT = "BID_DOCUMENT"; // 投标文件 (link)
  public static final String BID_WINNING_NOTICE = "BID_WINNING_NOTICE"; // 中标公示 (link)
  public static final String CONTRACT_DRAFT = "CONTRACT_DRAFT"; // 合同 (link)
  public static final String CONTRACT_REVIEW_MINUTES = "CONTRACT_REVIEW_MINUTES"; // 评审会议纪要 (report)
  public static final String REVIEW_EMAIL_ARCHIVE = "REVIEW_EMAIL_ARCHIVE"; // 邮件归档 (link)
  public static final String SIGNED_CONTRACT = "SIGNED_CONTRACT"; // 已盖章合同 (link)
  // v0.0.53 现场调研(SURVEY)-gate types：报告类正文 + 附件类 URL（现场照片/记录等外部材料）。
  public static final String SURVEY_REPORT = "SURVEY_REPORT"; // 现场调研报告 (report)
  public static final String SURVEY_ATTACHMENT = "SURVEY_ATTACHMENT"; // 现场调研附件 (link)
  // v0.0.57 交付实施(DELIVERY)-gate type：甲方验收报告（报告类，标题+富文本正文）。
  public static final String DELIVERY_ACCEPTANCE_REPORT = "DELIVERY_ACCEPTANCE_REPORT"; // 甲方验收报告 (report)

  public static final Set<String> ALL =
      Collections.unmodifiableSet(
          new HashSet<>(
              Arrays.asList(
                  RESEARCH_REPORT,
                  DECISION_MINUTES,
                  PRESENTATION_MATERIAL,
                  CLIENT_REQUIREMENTS,
                  POC_SCORE,
                  GAP_ANALYSIS,
                  BID_DOCUMENT,
                  BID_WINNING_NOTICE,
                  CONTRACT_DRAFT,
                  CONTRACT_REVIEW_MINUTES,
                  REVIEW_EMAIL_ARCHIVE,
                  SIGNED_CONTRACT,
                  SURVEY_REPORT,
                  SURVEY_ATTACHMENT,
                  DELIVERY_ACCEPTANCE_REPORT)));

  /** LINK-kind types carry a URL (the {@code link} field); others carry rich-text {@code content}. */
  public static final Set<String> LINK_TYPES =
      Collections.unmodifiableSet(
          new HashSet<>(
              Arrays.asList(
                  PRESENTATION_MATERIAL,
                  CLIENT_REQUIREMENTS,
                  BID_DOCUMENT,
                  BID_WINNING_NOTICE,
                  CONTRACT_DRAFT,
                  REVIEW_EMAIL_ARCHIVE,
                  SIGNED_CONTRACT,
                  SURVEY_ATTACHMENT)));

  private static final Map<String, String> LABELS;

  static {
    Map<String, String> m = new HashMap<>();
    m.put(RESEARCH_REPORT, "商机调研报告");
    m.put(DECISION_MINUTES, "决策评审纪要");
    m.put(PRESENTATION_MATERIAL, "讲解材料");
    m.put(CLIENT_REQUIREMENTS, "甲方诉求清单");
    m.put(POC_SCORE, "POC 得分表");
    m.put(GAP_ANALYSIS, "差距分析报告");
    m.put(BID_DOCUMENT, "投标文件");
    m.put(BID_WINNING_NOTICE, "中标公示");
    m.put(CONTRACT_DRAFT, "合同");
    m.put(CONTRACT_REVIEW_MINUTES, "评审会议纪要");
    m.put(REVIEW_EMAIL_ARCHIVE, "邮件归档");
    m.put(SIGNED_CONTRACT, "已盖章合同");
    m.put(SURVEY_REPORT, "现场调研报告");
    m.put(SURVEY_ATTACHMENT, "现场调研附件");
    m.put(DELIVERY_ACCEPTANCE_REPORT, "甲方验收报告");
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
