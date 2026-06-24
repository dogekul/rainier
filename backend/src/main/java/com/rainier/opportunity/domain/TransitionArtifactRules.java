/* (C) 2026 Rainier — internal use only. */
package com.rainier.opportunity.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Configurable「来源阶段 → 推进所需的产出物类型列表」rules (v0.0.45/46). {@code advance()} always advances FROM
 * {@code opportunity.stage}; a gate decision is also "advance from this stage", so rules are keyed by the
 * SOURCE stage. Each stage may require SEVERAL artifact types (all must exist to advance). Adding a
 * deliverable for 立项 later = add an entry here; the advance logic is untouched.
 *
 * <p>v0.0.46: 门禁默认仅对前进（PASS / 非关口）强制——关口 REJECT（丢单）不要求前进交付物，唯
 * {@link #requiredOnReject} 标注的阶段（商机决策）在通过/否决都要留痕。
 */
public final class TransitionArtifactRules {

  private static final Map<String, List<String>> RULES;

  /**
   * Stages whose 产出物 is a decision record required on BOTH PASS and REJECT (vs forward-only
   * deliverables which only gate PASS). Only 商机决策 (OPPORTUNITY → 《决策评审纪要》) qualifies.
   */
  private static final Set<String> REQUIRED_ON_REJECT =
      Collections.unmodifiableSet(
          new HashSet<>(Collections.singletonList(OpportunityStage.OPPORTUNITY)));

  static {
    Map<String, List<String>> m = new HashMap<>();
    m.put(OpportunityStage.LEAD, Collections.singletonList(ArtifactType.RESEARCH_REPORT)); // 线索→商机
    m.put(OpportunityStage.OPPORTUNITY, Collections.singletonList(ArtifactType.DECISION_MINUTES)); // 商机决策
    // 推介/POC → 投标：讲解材料 + 甲方诉求清单 + POC 得分表 + 差距分析报告 全齐才能进投标。
    m.put(
        OpportunityStage.POC,
        Collections.unmodifiableList(
            Arrays.asList(
                ArtifactType.PRESENTATION_MATERIAL,
                ArtifactType.CLIENT_REQUIREMENTS,
                ArtifactType.POC_SCORE,
                ArtifactType.GAP_ANALYSIS)));
    // v0.0.46 投标 → 合同（中标）：投标文件（≥1，可多份）。
    m.put(OpportunityStage.BIDDING, Collections.singletonList(ArtifactType.BID_DOCUMENT));
    // v0.0.46 合同签订 → 立项：中标公示 + 合同 + 评审会议纪要 + 邮件归档 + 已盖章合同 全齐才能进立项。
    m.put(
        OpportunityStage.CONTRACT,
        Collections.unmodifiableList(
            Arrays.asList(
                ArtifactType.BID_WINNING_NOTICE,
                ArtifactType.CONTRACT_DRAFT,
                ArtifactType.CONTRACT_REVIEW_MINUTES,
                ArtifactType.REVIEW_EMAIL_ARCHIVE,
                ArtifactType.SIGNED_CONTRACT)));
    // v0.0.53 现场调研 → 产品诉求：现场调研报告 + 现场调研附件 全齐才能推进（非关口，仅前进强制）。
    m.put(
        OpportunityStage.SURVEY,
        Collections.unmodifiableList(
            Arrays.asList(ArtifactType.SURVEY_REPORT, ArtifactType.SURVEY_ATTACHMENT)));
    RULES = Collections.unmodifiableMap(m);
  }

  /** The artifact types required to advance FROM {@code stage} (empty if none). */
  public static List<String> requiredFor(String stage) {
    return RULES.getOrDefault(stage, Collections.emptyList());
  }

  /** Whether {@code stage}'s required artifacts are also enforced on a gate REJECT (decision record). */
  public static boolean requiredOnReject(String stage) {
    return REQUIRED_ON_REJECT.contains(stage);
  }

  private TransitionArtifactRules() {}
}
