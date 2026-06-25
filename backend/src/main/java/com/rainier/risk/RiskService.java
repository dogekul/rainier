/* (C) 2026 Rainier — internal use only. */
package com.rainier.risk;

import com.rainier.portfolio.ScopeService;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fans the active set of {@link RiskRule} components out across the caller's project scope and
 * concatenates every {@link RiskFinding} they produce. Pure read aggregation — zero writes. v0.0.70
 * is intentionally rule-only (no AI inference); later phases may register additional rules without
 * changing the public {@code runAll} contract.
 */
@Service
@Transactional(readOnly = true)
public class RiskService {

  private final List<RiskRule> rules;
  private final ScopeService scopeService;
  private final UserRepository userRepo;

  public RiskService(List<RiskRule> rules, ScopeService scopeService, UserRepository userRepo) {
    this.rules = rules;
    this.scopeService = scopeService;
    this.userRepo = userRepo;
  }

  public List<RiskFinding> runAll(String username, String scope) {
    List<RiskFinding> all = new ArrayList<RiskFinding>();
    if (username == null || username.isEmpty()) {
      return all;
    }
    User me = userRepo.findByLoginName(username).orElse(null);
    if (me == null) {
      return all;
    }
    List<Long> projectIds = scopeService.resolveProjectIds(username, scope);
    if (projectIds.isEmpty()) {
      return all;
    }
    RiskContext ctx = new RiskContext(me.getId(), projectIds, LocalDateTime.now());
    for (RiskRule rule : rules) {
      List<RiskFinding> found = rule.evaluate(ctx);
      if (found != null && !found.isEmpty()) {
        all.addAll(found);
      }
    }
    return all;
  }
}
