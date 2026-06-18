/* (C) 2026 Rainier — internal use only. */
package com.rainier.me.service;

import com.rainier.common.domain.Priority;
import com.rainier.me.dto.PendingReview;
import com.rainier.project.domain.Project;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.sprint.domain.Sprint;
import com.rainier.sprint.repository.SprintRepository;
import com.rainier.story.domain.ReviewStatus;
import com.rainier.story.domain.Story;
import com.rainier.story.repository.StoryRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The「我的待评审队列」read-model (v0.0.39): stories where the caller is the assigned reviewer and the
 * review is still PENDING. Mirrors the {@code /api/me/*} self-scoped substrate (PortfolioService).
 * Batch-enriches owner / project / sprint (no N+1) and sorts priority-high-first then oldest-waiting.
 */
@Service
@Transactional(readOnly = true)
public class MeReviewService {

  /** Priority rank for sorting: lower = more urgent; unknown sorts last. */
  private static final List<String> PRIORITY_ORDER =
      Arrays.asList(Priority.URGENT, Priority.HIGH, Priority.MEDIUM, Priority.LOW, Priority.LOWEST);

  private final UserRepository userRepo;
  private final StoryRepository storyRepo;
  private final ProjectRepository projectRepo;
  private final SprintRepository sprintRepo;

  public MeReviewService(
      UserRepository userRepo,
      StoryRepository storyRepo,
      ProjectRepository projectRepo,
      SprintRepository sprintRepo) {
    this.userRepo = userRepo;
    this.storyRepo = storyRepo;
    this.projectRepo = projectRepo;
    this.sprintRepo = sprintRepo;
  }

  /** Stories awaiting review by {@code username}; empty when the user is unknown. */
  public List<PendingReview> pendingReviews(String username) {
    User me = userRepo.findByLoginName(username).orElse(null);
    if (me == null) {
      return new ArrayList<>();
    }
    List<Story> stories =
        storyRepo.findByReviewerUserIdAndReviewStatus(me.getId(), ReviewStatus.PENDING);
    if (stories.isEmpty()) {
      return new ArrayList<>();
    }
    Set<Long> userIds =
        stories.stream()
            .map(Story::getOwnerUserId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(HashSet::new));
    Set<Long> projectIds =
        stories.stream()
            .map(Story::getProjectId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(HashSet::new));
    Set<Long> sprintIds =
        stories.stream()
            .map(Story::getSprintId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(HashSet::new));
    Map<Long, User> userMap = batchById(userRepo.findAllById(userIds), User::getId);
    Map<Long, Project> projectMap =
        projectIds.isEmpty()
            ? new HashMap<>()
            : batchById(projectRepo.findAllById(projectIds), Project::getId);
    Map<Long, Sprint> sprintMap =
        sprintIds.isEmpty()
            ? new HashMap<>()
            : batchById(sprintRepo.findAllById(sprintIds), Sprint::getId);

    List<PendingReview> rows =
        stories.stream()
            .map(s -> enrich(s, userMap, projectMap, sprintMap))
            .collect(Collectors.toCollection(ArrayList::new));
    rows.sort(
        Comparator.comparingInt((PendingReview r) -> priorityRank(r.getPriority()))
            .thenComparing(
                PendingReview::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder())));
    return rows;
  }

  private static int priorityRank(String priority) {
    int idx = PRIORITY_ORDER.indexOf(priority);
    return idx < 0 ? PRIORITY_ORDER.size() : idx;
  }

  private static <T> Map<Long, T> batchById(Iterable<T> entities, Function<T, Long> idExtractor) {
    Map<Long, T> map = new HashMap<>();
    for (T entity : entities) {
      map.put(idExtractor.apply(entity), entity);
    }
    return map;
  }

  private PendingReview enrich(
      Story s, Map<Long, User> userMap, Map<Long, Project> projectMap, Map<Long, Sprint> sprintMap) {
    PendingReview r = PendingReview.from(s);
    User owner = userMap.get(s.getOwnerUserId());
    if (owner != null) {
      r.setOwnerName(owner.getName());
      r.setOwnerLoginName(owner.getLoginName());
    }
    if (s.getProjectId() != null) {
      Project p = projectMap.get(s.getProjectId());
      if (p != null) {
        r.setProjectName(p.getName());
      }
    }
    Sprint sprint = sprintMap.get(s.getSprintId());
    if (sprint != null) {
      r.setSprintName(sprint.getName());
    }
    return r;
  }
}
