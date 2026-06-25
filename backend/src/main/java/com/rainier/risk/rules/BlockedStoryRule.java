/* (C) 2026 Rainier — internal use only. */
package com.rainier.risk.rules;

import com.rainier.risk.RiskContext;
import com.rainier.risk.RiskFinding;
import com.rainier.risk.RiskRule;
import com.rainier.story.domain.Story;
import com.rainier.story.domain.StoryStatus;
import com.rainier.story.repository.StoryRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * CRIT every Story whose status is {@link StoryStatus#BLOCKED} within {@code ctx.projectIds}.
 * BLOCKED is a first-class state (not a closeReason) so direct status comparison suffices.
 */
@Component
public class BlockedStoryRule implements RiskRule {

  private final StoryRepository storyRepo;

  public BlockedStoryRule(StoryRepository storyRepo) {
    this.storyRepo = storyRepo;
  }

  @Override
  public String name() {
    return "BlockedStoryRule";
  }

  @Override
  public List<RiskFinding> evaluate(RiskContext ctx) {
    List<RiskFinding> out = new ArrayList<RiskFinding>();
    if (ctx == null || ctx.getProjectIds().isEmpty()) {
      return out;
    }
    List<Story> stories = storyRepo.findByProjectIdIn(ctx.getProjectIds());
    for (Story s : stories) {
      if (!StoryStatus.BLOCKED.equals(s.getStatus())) {
        continue;
      }
      String msg = "Story #" + s.getId() + " 处于 BLOCKED 状态";
      out.add(new RiskFinding(RiskFinding.LEVEL_CRIT, msg, "STORY", s.getId(), name()));
    }
    return out;
  }
}
