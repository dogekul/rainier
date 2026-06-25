/* (C) 2026 Rainier — internal use only. */
package com.rainier.task.service;

import com.rainier.common.domain.Priority;
import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.ConflictException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.project.domain.Project;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.requirement.domain.Requirement;
import com.rainier.requirement.repository.RequirementRepository;
import com.rainier.sprint.domain.Sprint;
import com.rainier.sprint.repository.SprintRepository;
import com.rainier.story.domain.ReviewStatus;
import com.rainier.story.domain.Story;
import com.rainier.story.repository.StoryRepository;
import com.rainier.task.domain.Task;
import com.rainier.task.domain.TaskStatus;
import com.rainier.task.dto.TaskCreateRequest;
import com.rainier.task.dto.TaskDetail;
import com.rainier.task.dto.TaskUpdateRequest;
import com.rainier.task.repository.TaskRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business operations for {@link Task}. v0.0.11 invariants:
 *
 * <ul>
 *   <li>Create: projectId NN; sprintId/storyId/assigneeUserId optional. Cross-layer consistency
 *       enforced (sprint.req.proj == task.proj; story.proj == task.proj; story.sprintId ==
 *       task.sprintId when both set).
 *   <li>Update: parents (project/sprint/story) immutable. Owner mutable to any live user OR null
 *       (unassign).
 *   <li>code service-level unique (no DB UNIQUE).
 *   <li>list uses batch enrich (Decision 6) — 5 SELECTs + 2 page = 7 total per perf test.
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class TaskService {

  private final TaskRepository repo;
  private final ProjectRepository projectRepo;
  private final SprintRepository sprintRepo;
  private final StoryRepository storyRepo;
  private final RequirementRepository requirementRepo;
  private final UserRepository userRepo;

  public TaskService(
      TaskRepository repo,
      ProjectRepository projectRepo,
      SprintRepository sprintRepo,
      StoryRepository storyRepo,
      RequirementRepository requirementRepo,
      UserRepository userRepo) {
    this.repo = repo;
    this.projectRepo = projectRepo;
    this.sprintRepo = sprintRepo;
    this.storyRepo = storyRepo;
    this.requirementRepo = requirementRepo;
    this.userRepo = userRepo;
  }

  @Transactional
  public TaskDetail create(TaskCreateRequest req) {
    // 1. project must exist
    if (!projectRepo.existsById(req.getProjectId())) {
      throw new BadRequestException("project not found: id=" + req.getProjectId());
    }
    // 2. sprint must exist if set
    Sprint sprint = null;
    if (req.getSprintId() != null) {
      sprint =
          sprintRepo
              .findById(req.getSprintId())
              .orElseThrow(
                  () -> new BadRequestException("sprint not found: id=" + req.getSprintId()));
    }
    // 3. story must exist if set
    Story story = null;
    if (req.getStoryId() != null) {
      story =
          storyRepo
              .findById(req.getStoryId())
              .orElseThrow(
                  () -> new BadRequestException("story not found: id=" + req.getStoryId()));
    }
    // 4. assignee must exist if set
    if (req.getAssigneeUserId() != null && !userRepo.existsById(req.getAssigneeUserId())) {
      throw new BadRequestException("assignee user not found: id=" + req.getAssigneeUserId());
    }
    // 5. cross-layer consistency (Decision 2)
    if (sprint != null) {
      // Soft-deleted Requirement (parent of an alive Sprint) → @Where filter returns empty;
      // surface a precise message instead of the generic "sprint not in project".
      final Long sprintIdForLambda = sprint.getId();
      Requirement parentReq =
          requirementRepo
              .findById(sprint.getRequirementId())
              .orElseThrow(
                  () ->
                      new BadRequestException(
                          "sprint parent requirement missing: sprintId=" + sprintIdForLambda));
      if (!Objects.equals(parentReq.getProjectId(), req.getProjectId())) {
        throw new BadRequestException("sprint not in project");
      }
    }
    if (story != null) {
      if (!Objects.equals(story.getProjectId(), req.getProjectId())) {
        throw new BadRequestException("story not in project");
      }
      if (req.getSprintId() != null && !Objects.equals(story.getSprintId(), req.getSprintId())) {
        throw new BadRequestException("story not in sprint");
      }
    }
    // 6. code uniqueness
    if (repo.existsByCode(req.getCode())) {
      throw new ConflictException("code already exists: " + req.getCode());
    }
    // 7. status / priority defaults + validation
    String status = req.getStatus() == null ? TaskStatus.TODO : req.getStatus();
    if (!TaskStatus.ALL.contains(status)) {
      throw new BadRequestException("invalid status: " + status);
    }
    String priority = req.getPriority() == null ? Priority.MEDIUM : req.getPriority();
    if (!Priority.ALL.contains(priority)) {
      throw new BadRequestException("invalid priority: " + priority);
    }
    validateReviewFields(req.getReviewerUserId(), req.getReviewStatus());
    Task t = new Task();
    t.setCode(req.getCode());
    t.setTitle(req.getTitle());
    t.setDescription(req.getDescription());
    t.setStatus(status);
    t.setPriority(priority);
    t.setProjectId(req.getProjectId());
    t.setSprintId(req.getSprintId());
    t.setStoryId(req.getStoryId());
    t.setAssigneeUserId(req.getAssigneeUserId());
    t.setDueDate(req.getDueDate());
    t.setCloseReason(req.getCloseReason());
    t.setReviewerUserId(req.getReviewerUserId());
    t.setReviewStatus(req.getReviewStatus());
    return enrich(repo.saveAndFlush(t));
  }

  /** v0.0.82: validate optional review fields — reviewer must exist; status must be in ALL. */
  private void validateReviewFields(Long reviewerUserId, String reviewStatus) {
    if (reviewerUserId != null && !userRepo.existsById(reviewerUserId)) {
      throw new BadRequestException("reviewer user not found: id=" + reviewerUserId);
    }
    if (reviewStatus != null && !ReviewStatus.ALL.contains(reviewStatus)) {
      throw new BadRequestException("invalid reviewStatus: " + reviewStatus);
    }
  }

  /**
   * v0.0.82: record a review decision on a Task. {@code decision} must be APPROVED / REJECTED;
   * when REJECTED, {@code reason} is required (≤500) and is persisted to {@code closeReason}.
   * Reviewer assignment is left untouched.
   */
  @Transactional
  public TaskDetail review(Long id, String decision, String reason) {
    if (decision == null || !ReviewStatus.DECISIONS.contains(decision)) {
      throw new BadRequestException("invalid decision: " + decision);
    }
    if (ReviewStatus.REJECTED.equals(decision) && (reason == null || reason.trim().isEmpty())) {
      throw new BadRequestException("reason required for REJECTED");
    }
    Task t = getOrThrow(id);
    t.setReviewStatus(decision);
    if (ReviewStatus.REJECTED.equals(decision)) {
      t.setCloseReason(reason);
    }
    return enrich(repo.saveAndFlush(t));
  }

  public TaskDetail findById(Long id) {
    return enrich(getOrThrow(id));
  }

  public PageResponse<TaskDetail> list(
      Long projectId,
      Long sprintId,
      Long storyId,
      String status,
      String priority,
      Long assigneeUserId,
      PageParams page) {
    Specification<Task> spec =
        (root, query, cb) -> {
          javax.persistence.criteria.Predicate p = cb.conjunction();
          if (projectId != null) {
            p = cb.and(p, cb.equal(root.get("projectId"), projectId));
          }
          if (sprintId != null) {
            p = cb.and(p, cb.equal(root.get("sprintId"), sprintId));
          }
          if (storyId != null) {
            p = cb.and(p, cb.equal(root.get("storyId"), storyId));
          }
          if (status != null) {
            p = cb.and(p, cb.equal(root.get("status"), status));
          }
          if (priority != null) {
            p = cb.and(p, cb.equal(root.get("priority"), priority));
          }
          if (assigneeUserId != null) {
            p = cb.and(p, cb.equal(root.get("assigneeUserId"), assigneeUserId));
          }
          String search = page.getSearch();
          if (search != null && !search.isEmpty()) {
            String pattern = "%" + search.toLowerCase() + "%";
            p =
                cb.and(
                    p,
                    cb.or(
                        cb.like(cb.lower(root.get("code")), pattern),
                        cb.like(cb.lower(root.get("title")), pattern)));
          }
          return p;
        };
    PageRequest pr =
        PageRequest.of(page.getPage(), page.getSize(), Sort.by(Sort.Direction.DESC, "createTime"));
    Page<Task> result = repo.findAll(spec, pr);
    List<Task> tasks = result.getContent();
    if (tasks.isEmpty()) {
      return PageResponse.of(
          Collections.emptyList(), page.getPage(), page.getSize(), result.getTotalElements());
    }
    // Decision 6: 4-stage batch enrich. SQL count locked to 6 (2 page + 4 batches).
    // PA-1 revision: Requirement is NOT surfaced in TaskDetail fields, so no Requirement batch.
    Set<Long> userIds =
        tasks.stream()
            .map(Task::getAssigneeUserId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(HashSet::new));
    // v0.0.82: reviewers join the same user batch so reviewerName enriches without extra queries.
    tasks.stream().map(Task::getReviewerUserId).filter(Objects::nonNull).forEach(userIds::add);
    Set<Long> sprintIds =
        tasks.stream()
            .map(Task::getSprintId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(HashSet::new));
    Set<Long> storyIds =
        tasks.stream()
            .map(Task::getStoryId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(HashSet::new));
    Set<Long> projectIds =
        tasks.stream()
            .map(Task::getProjectId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(HashSet::new));
    // requirementIds is harvested AFTER sprintMap is built (Decision 6 — derived deps).
    Map<Long, User> userMap =
        userIds.isEmpty()
            ? Collections.emptyMap()
            : batchById(userRepo.findAllById(userIds), User::getId);
    Map<Long, Sprint> sprintMap =
        sprintIds.isEmpty()
            ? Collections.emptyMap()
            : batchById(sprintRepo.findAllById(sprintIds), Sprint::getId);
    Map<Long, Story> storyMap =
        storyIds.isEmpty()
            ? Collections.emptyMap()
            : batchById(storyRepo.findAllById(storyIds), Story::getId);
    Map<Long, Project> projectMap =
        projectIds.isEmpty()
            ? Collections.emptyMap()
            : batchById(projectRepo.findAllById(projectIds), Project::getId);
    // No Requirement batch — TaskDetail does not expose requirement fields directly.
    return PageResponse.of(
        tasks.stream()
            .map(t -> enrichBatch(t, userMap, sprintMap, storyMap, projectMap))
            .collect(Collectors.toList()),
        page.getPage(),
        page.getSize(),
        result.getTotalElements());
  }

  @Transactional
  public TaskDetail update(Long id, TaskUpdateRequest req) {
    Task t = getOrThrow(id);
    if (!TaskStatus.ALL.contains(req.getStatus())) {
      throw new BadRequestException("invalid status: " + req.getStatus());
    }
    if (!Priority.ALL.contains(req.getPriority())) {
      throw new BadRequestException("invalid priority: " + req.getPriority());
    }
    // assignee may be cleared to null (unassign), or changed to another live user
    Long newAssignee = req.getAssigneeUserId();
    if (newAssignee != null
        && !Objects.equals(newAssignee, t.getAssigneeUserId())
        && !userRepo.existsById(newAssignee)) {
      throw new BadRequestException("assignee user not found: id=" + newAssignee);
    }
    t.setAssigneeUserId(newAssignee);
    if (!req.getCode().equals(t.getCode())) {
      if (repo.existsByCode(req.getCode())) {
        throw new ConflictException("code already exists: " + req.getCode());
      }
      t.setCode(req.getCode());
    }
    t.setTitle(req.getTitle());
    t.setDescription(req.getDescription());
    t.setStatus(req.getStatus());
    t.setPriority(req.getPriority());
    t.setDueDate(req.getDueDate());
    t.setCloseReason(req.getCloseReason());
    // v0.0.82: reviewer fields are patch-like — only touch when client explicitly sent the key
    // (mirrors Story v0.0.81 fix). Absent → keep original; explicit null → clear; value → replace.
    if (req.isReviewerUserIdSet()) {
      if (req.getReviewerUserId() != null && !userRepo.existsById(req.getReviewerUserId())) {
        throw new BadRequestException("reviewer user not found: id=" + req.getReviewerUserId());
      }
      t.setReviewerUserId(req.getReviewerUserId());
    }
    if (req.isReviewStatusSet()) {
      if (req.getReviewStatus() != null && !ReviewStatus.ALL.contains(req.getReviewStatus())) {
        throw new BadRequestException("invalid reviewStatus: " + req.getReviewStatus());
      }
      t.setReviewStatus(req.getReviewStatus());
    }
    // projectId / sprintId / storyId intentionally NOT touched — immutable after creation.
    return enrich(repo.saveAndFlush(t));
  }

  @Transactional
  public void delete(Long id) {
    Task t = getOrThrow(id);
    repo.delete(t);
  }

  Task getOrThrow(Long id) {
    return repo.findById(id).orElseThrow(() -> new NotFoundException("task not found: id=" + id));
  }

  // ----- enrich helpers -----

  /** Single-row enrich path — used by create/update/findById. */
  private TaskDetail enrich(Task t) {
    TaskDetail dto = TaskDetail.from(t);
    if (t.getAssigneeUserId() != null) {
      userRepo
          .findById(t.getAssigneeUserId())
          .ifPresent(
              u -> {
                dto.setAssigneeName(u.getName());
                dto.setAssigneeLoginName(u.getLoginName());
              });
    }
    if (t.getReviewerUserId() != null) {
      userRepo
          .findById(t.getReviewerUserId())
          .ifPresent(u -> dto.setReviewerName(u.getName()));
    }
    if (t.getSprintId() != null) {
      sprintRepo
          .findById(t.getSprintId())
          .ifPresent(
              s -> {
                dto.setSprintCode(s.getCode());
                dto.setSprintName(s.getName());
              });
    }
    if (t.getStoryId() != null) {
      storyRepo
          .findById(t.getStoryId())
          .ifPresent(
              s -> {
                dto.setStoryCode(s.getCode());
                dto.setStoryTitle(s.getTitle());
              });
    }
    projectRepo
        .findById(t.getProjectId())
        .ifPresent(
            p -> {
              dto.setProjectName(p.getName());
              dto.setProjectCode(p.getCode());
            });
    return dto;
  }

  private static <T> Map<Long, T> batchById(Iterable<T> entities, Function<T, Long> idExtractor) {
    Map<Long, T> map = new HashMap<>();
    for (T entity : entities) {
      map.put(idExtractor.apply(entity), entity);
    }
    return map;
  }

  private TaskDetail enrichBatch(
      Task t,
      Map<Long, User> userMap,
      Map<Long, Sprint> sprintMap,
      Map<Long, Story> storyMap,
      Map<Long, Project> projectMap) {
    TaskDetail dto = TaskDetail.from(t);
    if (t.getAssigneeUserId() != null) {
      User u = userMap.get(t.getAssigneeUserId());
      if (u != null) {
        dto.setAssigneeName(u.getName());
        dto.setAssigneeLoginName(u.getLoginName());
      }
    }
    if (t.getReviewerUserId() != null) {
      User rv = userMap.get(t.getReviewerUserId());
      if (rv != null) {
        dto.setReviewerName(rv.getName());
      }
    }
    if (t.getSprintId() != null) {
      Sprint s = sprintMap.get(t.getSprintId());
      if (s != null) {
        dto.setSprintCode(s.getCode());
        dto.setSprintName(s.getName());
      }
    }
    if (t.getStoryId() != null) {
      Story s = storyMap.get(t.getStoryId());
      if (s != null) {
        dto.setStoryCode(s.getCode());
        dto.setStoryTitle(s.getTitle());
      }
    }
    Project p = projectMap.get(t.getProjectId());
    if (p != null) {
      dto.setProjectName(p.getName());
      dto.setProjectCode(p.getCode());
    }
    return dto;
  }
}
