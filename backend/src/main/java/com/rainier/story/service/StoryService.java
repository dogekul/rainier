/* (C) 2026 Rainier — internal use only. */
package com.rainier.story.service;

import com.rainier.common.domain.Priority;
import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.ConflictException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.project.domain.Project;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.requirement.domain.Complexity;
import com.rainier.requirement.domain.Requirement;
import com.rainier.requirement.repository.RequirementRepository;
import com.rainier.sprint.domain.Sprint;
import com.rainier.sprint.repository.SprintRepository;
import com.rainier.story.domain.Story;
import com.rainier.story.domain.StoryStatus;
import com.rainier.story.dto.StoryCreateRequest;
import com.rainier.story.dto.StoryDetail;
import com.rainier.story.dto.StoryUpdateRequest;
import com.rainier.story.repository.StoryRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import java.util.Collections;
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
 * Business operations for {@link Story}.
 *
 * <p>Invariants:
 *
 * <ul>
 *   <li>Create-only fields: {@code requirementId} and {@code projectId} (projectId is copied from
 *       parent Requirement and never accepted from the client).
 *   <li>Mutable owner with existence validation (sibling of v0.0.8 Project / Requirement).
 *   <li>code uniqueness is service-level (no DB UNIQUE); soft-deleted codes can be reused — same
 *       family pattern as Project / Requirement / Demand.
 *   <li>Status / priority / complexity validated against in-memory ALL sets (Java 8 compat).
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class StoryService {

  private final StoryRepository repo;
  private final SprintRepository sprintRepo;
  private final RequirementRepository requirementRepo;
  private final UserRepository userRepo;
  private final ProjectRepository projectRepo;

  public StoryService(
      StoryRepository repo,
      SprintRepository sprintRepo,
      RequirementRepository requirementRepo,
      UserRepository userRepo,
      ProjectRepository projectRepo) {
    this.repo = repo;
    this.sprintRepo = sprintRepo;
    this.requirementRepo = requirementRepo;
    this.userRepo = userRepo;
    this.projectRepo = projectRepo;
  }

  @Transactional
  public StoryDetail create(StoryCreateRequest req) {
    Sprint sprint =
        sprintRepo
            .findById(req.getSprintId())
            .orElseThrow(
                () -> new BadRequestException("sprint not found: id=" + req.getSprintId()));
    if (!userRepo.existsById(req.getOwnerUserId())) {
      throw new BadRequestException("owner user not found: id=" + req.getOwnerUserId());
    }
    if (repo.existsByCode(req.getCode())) {
      throw new ConflictException("code already exists: " + req.getCode());
    }
    String status = req.getStatus() == null ? StoryStatus.DRAFT : req.getStatus();
    if (!StoryStatus.ALL.contains(status)) {
      throw new BadRequestException("invalid status: " + status);
    }
    String priority = req.getPriority() == null ? Priority.MEDIUM : req.getPriority();
    if (!Priority.ALL.contains(priority)) {
      throw new BadRequestException("invalid priority: " + priority);
    }
    if (req.getComplexity() != null && !Complexity.ALL.contains(req.getComplexity())) {
      throw new BadRequestException("invalid complexity: " + req.getComplexity());
    }
    Story s = new Story();
    s.setCode(req.getCode());
    s.setTitle(req.getTitle());
    s.setDescription(req.getDescription());
    s.setAcceptanceCriteria(req.getAcceptanceCriteria());
    s.setStatus(status);
    s.setPriority(priority);
    s.setComplexity(req.getComplexity());
    s.setSprintId(sprint.getId());
    // v0.0.10: projectId auto-inherited transitively from grandparent Requirement.
    Requirement parentReq = requirementRepo.findById(sprint.getRequirementId()).orElse(null);
    s.setProjectId(parentReq == null ? null : parentReq.getProjectId());
    s.setOwnerUserId(req.getOwnerUserId());
    s.setCloseReason(req.getCloseReason());
    return enrich(repo.saveAndFlush(s));
  }

  public StoryDetail findById(Long id) {
    return enrich(getOrThrow(id));
  }

  public PageResponse<StoryDetail> list(
      Long sprintId, String status, String priority, PageParams page) {
    Specification<Story> spec =
        (root, query, cb) -> {
          javax.persistence.criteria.Predicate p = cb.conjunction();
          if (sprintId != null) {
            p = cb.and(p, cb.equal(root.get("sprintId"), sprintId));
          }
          if (status != null) {
            p = cb.and(p, cb.equal(root.get("status"), status));
          }
          if (priority != null) {
            p = cb.and(p, cb.equal(root.get("priority"), priority));
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
    Page<Story> result = repo.findAll(spec, pr);
    List<Story> stories = result.getContent();
    if (stories.isEmpty()) {
      return PageResponse.of(
          Collections.emptyList(), page.getPage(), page.getSize(), result.getTotalElements());
    }
    // v0.0.10.1 Code-M1 fix: batch enrich — 4 SELECTs total (user, sprint, requirement, project)
    // replacing v0.0.10's ~80 per-row queries on a size=20 page.
    // Code-M2 defense: filter null defensively even though FK columns are NN at the entity layer
    // — a single legacy row with NULL would otherwise blow up the whole list with
    // InvalidDataAccessApiUsageException("ids must not contain null").
    Set<Long> userIds =
        stories.stream()
            .map(Story::getOwnerUserId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(HashSet::new));
    Set<Long> sprintIds =
        stories.stream()
            .map(Story::getSprintId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(HashSet::new));
    Map<Long, User> userMap = batchById(userRepo.findAllById(userIds), User::getId);
    Map<Long, Sprint> sprintMap = batchById(sprintRepo.findAllById(sprintIds), Sprint::getId);
    Set<Long> requirementIds =
        sprintMap.values().stream()
            .map(Sprint::getRequirementId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(HashSet::new));
    Map<Long, Requirement> requirementMap =
        requirementIds.isEmpty()
            ? Collections.emptyMap()
            : batchById(requirementRepo.findAllById(requirementIds), Requirement::getId);
    Set<Long> projectIds =
        stories.stream()
            .map(Story::getProjectId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(HashSet::new));
    Map<Long, Project> projectMap =
        projectIds.isEmpty()
            ? Collections.emptyMap()
            : batchById(projectRepo.findAllById(projectIds), Project::getId);
    return PageResponse.of(
        stories.stream()
            .map(s -> enrichBatch(s, userMap, sprintMap, requirementMap, projectMap))
            .collect(Collectors.toList()),
        page.getPage(),
        page.getSize(),
        result.getTotalElements());
  }

  private static <T> Map<Long, T> batchById(Iterable<T> entities, Function<T, Long> idExtractor) {
    Map<Long, T> map = new java.util.HashMap<>();
    for (T entity : entities) {
      map.put(idExtractor.apply(entity), entity);
    }
    return map;
  }

  private StoryDetail enrichBatch(
      Story s,
      Map<Long, User> userMap,
      Map<Long, Sprint> sprintMap,
      Map<Long, Requirement> requirementMap,
      Map<Long, Project> projectMap) {
    StoryDetail dto = StoryDetail.from(s);
    User u = userMap.get(s.getOwnerUserId());
    if (u != null) {
      dto.setOwnerName(u.getName());
      dto.setOwnerLoginName(u.getLoginName());
    }
    Sprint sprint = sprintMap.get(s.getSprintId());
    if (sprint != null) {
      dto.setSprintCode(sprint.getCode());
      dto.setSprintName(sprint.getName());
      dto.setSprintStatus(sprint.getStatus());
      dto.setRequirementId(sprint.getRequirementId());
      Requirement r = requirementMap.get(sprint.getRequirementId());
      if (r != null) {
        dto.setRequirementCode(r.getCode());
        dto.setRequirementTitle(r.getTitle());
      }
    }
    if (s.getProjectId() != null) {
      Project p = projectMap.get(s.getProjectId());
      if (p != null) {
        dto.setProjectName(p.getName());
        dto.setProjectCode(p.getCode());
      }
    }
    return dto;
  }

  @Transactional
  public StoryDetail update(Long id, StoryUpdateRequest req) {
    Story s = getOrThrow(id);
    if (!StoryStatus.ALL.contains(req.getStatus())) {
      throw new BadRequestException("invalid status: " + req.getStatus());
    }
    if (!Priority.ALL.contains(req.getPriority())) {
      throw new BadRequestException("invalid priority: " + req.getPriority());
    }
    if (req.getComplexity() != null && !Complexity.ALL.contains(req.getComplexity())) {
      throw new BadRequestException("invalid complexity: " + req.getComplexity());
    }
    if (!req.getOwnerUserId().equals(s.getOwnerUserId())) {
      if (!userRepo.existsById(req.getOwnerUserId())) {
        throw new BadRequestException("owner user not found: id=" + req.getOwnerUserId());
      }
      s.setOwnerUserId(req.getOwnerUserId());
    }
    if (!req.getCode().equals(s.getCode())) {
      if (repo.existsByCode(req.getCode())) {
        throw new ConflictException("code already exists: " + req.getCode());
      }
      s.setCode(req.getCode());
    }
    s.setTitle(req.getTitle());
    s.setDescription(req.getDescription());
    s.setAcceptanceCriteria(req.getAcceptanceCriteria());
    s.setStatus(req.getStatus());
    s.setPriority(req.getPriority());
    s.setComplexity(req.getComplexity());
    s.setCloseReason(req.getCloseReason());
    // requirementId / projectId intentionally NOT touched — immutable after creation.
    return enrich(repo.saveAndFlush(s));
  }

  @Transactional
  public void delete(Long id) {
    Story s = getOrThrow(id);
    repo.delete(s);
  }

  Story getOrThrow(Long id) {
    return repo.findById(id).orElseThrow(() -> new NotFoundException("story not found: id=" + id));
  }

  /**
   * v0.0.10: 2-stage join Sprint → Requirement → Project + owner join. Populates sprintCode /
   * sprintName / sprintStatus / requirementId / requirementCode / requirementTitle / projectName /
   * projectCode / ownerName / ownerLoginName. Defensive null handling for read path.
   */
  private StoryDetail enrich(Story s) {
    StoryDetail dto = StoryDetail.from(s);
    userRepo
        .findById(s.getOwnerUserId())
        .ifPresent(
            u -> {
              dto.setOwnerName(u.getName());
              dto.setOwnerLoginName(u.getLoginName());
            });
    Sprint sprint = sprintRepo.findById(s.getSprintId()).orElse(null);
    if (sprint != null) {
      dto.setSprintCode(sprint.getCode());
      dto.setSprintName(sprint.getName());
      dto.setSprintStatus(sprint.getStatus());
      dto.setRequirementId(sprint.getRequirementId());
      requirementRepo
          .findById(sprint.getRequirementId())
          .ifPresent(
              r -> {
                dto.setRequirementCode(r.getCode());
                dto.setRequirementTitle(r.getTitle());
              });
    }
    if (s.getProjectId() != null) {
      Project p = projectRepo.findById(s.getProjectId()).orElse(null);
      if (p != null) {
        dto.setProjectName(p.getName());
        dto.setProjectCode(p.getCode());
      }
    }
    return dto;
  }
}
