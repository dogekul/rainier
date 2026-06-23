/* (C) 2026 Rainier — internal use only. */
package com.rainier.opportunity.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configurable「来源阶段 → 推进所需的产出物类型列表」rules (v0.0.45). {@code advance()} always advances FROM
 * {@code opportunity.stage}; a gate decision is also "advance from this stage", so rules are keyed by the
 * SOURCE stage. Each stage may require SEVERAL artifact types (all must exist to advance). Adding a
 * deliverable for 投标/合同/立项 later = add an entry here; the advance logic is untouched.
 */
public final class TransitionArtifactRules {

  private static final Map<String, List<String>> RULES;

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
    RULES = Collections.unmodifiableMap(m);
  }

  /** The artifact types required to advance FROM {@code stage} (empty if none). */
  public static List<String> requiredFor(String stage) {
    return RULES.getOrDefault(stage, Collections.emptyList());
  }

  private TransitionArtifactRules() {}
}
