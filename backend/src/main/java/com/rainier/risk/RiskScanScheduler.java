/* (C) 2026 Rainier — internal use only. */
package com.rainier.risk;

import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodically scans enabled users' project scopes and lets RiskService push CRIT notifications. */
@Component
@ConditionalOnProperty(prefix = "app.risk.scan", name = "enabled", havingValue = "true")
public class RiskScanScheduler {

  private static final Logger log = LoggerFactory.getLogger(RiskScanScheduler.class);

  private final RiskService riskService;
  private final UserRepository userRepo;
  private final String scope;

  public RiskScanScheduler(
      RiskService riskService,
      UserRepository userRepo,
      @Value("${app.risk.scan.scope:mine}") String scope) {
    this.riskService = riskService;
    this.userRepo = userRepo;
    this.scope = scope == null || scope.trim().isEmpty() ? "mine" : scope.trim();
  }

  @Scheduled(
      initialDelayString = "${app.risk.scan.initial-delay-ms:60000}",
      fixedDelayString = "${app.risk.scan.fixed-delay-ms:300000}")
  public void scanAllUsers() {
    for (User user : userRepo.findAll()) {
      if (user == null
          || !Boolean.TRUE.equals(user.getEnabled())
          || user.getLoginName() == null
          || user.getLoginName().trim().isEmpty()) {
        continue;
      }
      try {
        riskService.runAll(user.getLoginName(), scope);
      } catch (RuntimeException ex) {
        log.warn("Risk scan failed for user {}", user.getLoginName(), ex);
      }
    }
  }
}
