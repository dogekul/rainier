/* (C) 2026 Rainier — internal use only. */
package com.rainier.risk;

import java.util.List;

/**
 * One scanning rule that turns the current {@link RiskContext} into zero-or-more {@link
 * RiskFinding}s. v0.0.70 — pure-rule implementations; later phases may layer AI inference behind the
 * same contract.
 */
public interface RiskRule {

  /** Stable identifier surfaced in {@link RiskFinding#getRuleName()}. */
  String name();

  List<RiskFinding> evaluate(RiskContext ctx);
}
