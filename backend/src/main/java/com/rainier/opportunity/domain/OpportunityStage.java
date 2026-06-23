/* (C) 2026 Rainier — internal use only. */
package com.rainier.opportunity.domain;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The customer-journey stages for an {@link Opportunity} (v0.0.44) — 客户全流程 售前环节 + 实施环节. The
 * opportunity walks the full pipeline: 线索→商机→推介/POC→投标→合同签订 (售前) then 立项→现场调研→产品诉求→
 * 交付实施→验收 (实施). The actual delivery WORK lives in a linked {@code Project} (实施=Project); these
 * stages track where the engagement is. {@link #GATE_STAGES} are the 4 decision gates (商机决策/投标决策/
 * 合同评审/立项评审): advancing FROM them requires a PASS/REJECT decision.
 */
public final class OpportunityStage {
  // 售前环节 (pre-sales)
  public static final String LEAD = "LEAD"; // 线索
  public static final String OPPORTUNITY = "OPPORTUNITY"; // 商机
  public static final String POC = "POC"; // 推介/POC
  public static final String BIDDING = "BIDDING"; // 投标
  public static final String CONTRACT = "CONTRACT"; // 合同签订
  // 实施环节 (delivery; the work runs in a linked Project)
  public static final String INITIATION = "INITIATION"; // 立项
  public static final String SURVEY = "SURVEY"; // 现场调研
  public static final String REQUIREMENT = "REQUIREMENT"; // 产品诉求
  public static final String DELIVERY = "DELIVERY"; // 交付实施
  public static final String ACCEPTANCE = "ACCEPTANCE"; // 验收

  /** Full pipeline order; advancing moves to the next index. */
  public static final List<String> STAGE_ORDER =
      Collections.unmodifiableList(
          Arrays.asList(
              LEAD, OPPORTUNITY, POC, BIDDING, CONTRACT, INITIATION, SURVEY, REQUIREMENT, DELIVERY,
              ACCEPTANCE));

  public static final Set<String> ALL = Collections.unmodifiableSet(new HashSet<>(STAGE_ORDER));

  /** Decision gates whose outward advance needs PASS/REJECT (商机/投标/合同/立项评审). */
  public static final Set<String> GATE_STAGES =
      Collections.unmodifiableSet(
          new HashSet<>(Arrays.asList(OPPORTUNITY, BIDDING, CONTRACT, INITIATION)));

  /** Pre-sales gates whose REJECT loses the deal (丢单). Rejecting 立项 instead just holds at 立项. */
  public static final Set<String> PRESALES_GATES =
      Collections.unmodifiableSet(new HashSet<>(Arrays.asList(OPPORTUNITY, BIDDING, CONTRACT)));

  private OpportunityStage() {}
}
