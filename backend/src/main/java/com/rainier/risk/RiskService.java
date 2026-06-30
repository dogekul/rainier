/* (C) 2026 Rainier — internal use only. */
package com.rainier.risk;

import com.rainier.notification.repository.NotificationRepository;
import com.rainier.notification.service.NotificationService;
import com.rainier.portfolio.ScopeService;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
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
  private final NotificationService notificationService;
  private final NotificationRepository notificationRepo;
  private final long notificationSuppressHours;

  public RiskService(
      List<RiskRule> rules,
      ScopeService scopeService,
      UserRepository userRepo,
      NotificationService notificationService,
      NotificationRepository notificationRepo,
      @Value("${app.risk.notifications.suppress-hours:24}") long notificationSuppressHours) {
    this.rules = rules;
    this.scopeService = scopeService;
    this.userRepo = userRepo;
    this.notificationService = notificationService;
    this.notificationRepo = notificationRepo;
    this.notificationSuppressHours = notificationSuppressHours;
  }

  @Transactional
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
    pushCritFindings(me.getId(), all);
    return all;
  }

  /**
   * v0.0.72 (A8): 对每条 CRIT finding 旁路写入一条站内通知。
   *
   * <p>v0.0.117: suppress duplicate unread notifications for the same user/rule/entity/title within
   * a short window. Once a user marks the notification read, a later scan may surface the risk again.
   */
  private void pushCritFindings(Long userId, List<RiskFinding> findings) {
    if (notificationService == null || userId == null || findings == null || findings.isEmpty()) {
      return;
    }
    for (RiskFinding f : findings) {
      if (f == null || !RiskFinding.LEVEL_CRIT.equals(f.getLevel())) {
        continue;
      }
      String title = "风险: " + (f.getMessage() == null ? f.getRuleName() : f.getMessage());
      String body = f.getRuleName() == null ? null : ("rule=" + f.getRuleName());
      if (hasRecentUnreadDuplicate(userId, f, title)) {
        continue;
      }
      notificationService.send(
          userId, title, body, RiskFinding.LEVEL_CRIT, f.getEntityType(), f.getEntityId());
    }
  }

  private boolean hasRecentUnreadDuplicate(Long userId, RiskFinding f, String title) {
    if (notificationRepo == null || notificationSuppressHours <= 0) {
      return false;
    }
    LocalDateTime threshold = LocalDateTime.now().minusHours(notificationSuppressHours);
    return notificationRepo
        .existsByUserIdAndLevelAndEntityTypeAndEntityIdAndTitleAndReadAtIsNullAndCreatedAtAfter(
            userId,
            RiskFinding.LEVEL_CRIT,
            f.getEntityType(),
            f.getEntityId(),
            title,
            threshold);
  }
}
