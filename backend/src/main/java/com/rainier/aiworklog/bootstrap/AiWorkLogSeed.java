/* (C) 2026 Rainier — internal use only. */
package com.rainier.aiworklog.bootstrap;

import com.rainier.aiworklog.domain.AiWorkLog;
import com.rainier.aiworklog.domain.AiWorkLogStatus;
import com.rainier.aiworklog.repository.AiWorkLogRepository;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * v0.0.43 — seed-driven flywheel shell. With no real AI/external integration yet, this seeds a few
 * PROPOSED {@link AiWorkLog} entries so the「AI 工作日志」page has data to review. Gated on
 * {@code app.demo.ai-work-log-seed.enabled} (false in the test profile so tests get a clean table) and
 * idempotent (only seeds when the table is empty).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AiWorkLogSeed implements CommandLineRunner {

  private static final Logger LOG = LoggerFactory.getLogger(AiWorkLogSeed.class);

  private final boolean enabled;
  private final AiWorkLogRepository repo;

  public AiWorkLogSeed(
      @Value("${app.demo.ai-work-log-seed.enabled:true}") boolean enabled,
      AiWorkLogRepository repo) {
    this.enabled = enabled;
    this.repo = repo;
  }

  @Override
  @Transactional
  public void run(String... args) {
    if (!enabled) {
      return;
    }
    if (repo.count() > 0) {
      return; // idempotent: already seeded
    }
    List<AiWorkLog> seeds = new ArrayList<>();
    seeds.add(
        make(
            "STATUS_SYNC",
            "UPDATE_TASK_STATUS",
            "TASK",
            "建议将任务「登录页联调」从 进行中 → 已完成",
            "检测到关联分支 commit abc1234 关闭该任务 + PR #42 已合并"));
    seeds.add(
        make(
            "RISK_RADAR",
            "FLAG_RISK",
            "PROJECT",
            "建议标记项目「采购系统重构」为风险：里程碑逾期且阻塞任务上升",
            "里程碑 M2 逾期 6 天；阻塞任务 3→7（近 7 天）"));
    seeds.add(
        make(
            "WEEKLY_REPORT",
            "DRAFT_WEEKLY",
            null,
            "已起草本周周报草稿（待确认后发送）",
            "汇总 12 条已完成任务 + 3 个里程碑进展 + 2 项风险"));
    seeds.add(
        make(
            "ASSIGNMENT",
            "SUGGEST_ASSIGNEE",
            "TASK",
            "建议将任务「导出报表性能优化」指派给 王伟",
            "王伟近 30 天关闭 8 个同模块任务，当前负载 2/5"));
    repo.saveAll(seeds);
    LOG.info("AiWorkLogSeed: seeded {} PROPOSED AI work logs", seeds.size());
  }

  private AiWorkLog make(
      String agentType, String action, String targetType, String summary, String evidence) {
    AiWorkLog a = new AiWorkLog();
    a.setAgentType(agentType);
    a.setAction(action);
    a.setTargetType(targetType);
    a.setSummary(summary);
    a.setEvidence(evidence);
    a.setStatus(AiWorkLogStatus.PROPOSED);
    return a;
  }
}
