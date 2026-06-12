/* (C) 2026 Rainier — internal use only. */
package com.rainier.milestone.service;

import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.ConflictException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.milestone.domain.Milestone;
import com.rainier.milestone.domain.MilestoneStatus;
import com.rainier.milestone.dto.MilestoneCreateRequest;
import com.rainier.milestone.dto.MilestoneDetail;
import com.rainier.milestone.dto.MilestoneUpdateRequest;
import com.rainier.milestone.repository.MilestoneRepository;
import com.rainier.project.repository.ProjectRepository;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business operations for {@link Milestone}.
 *
 * <ul>
 *   <li>{@code (projectId, code)} unique at service level (different projects may share a code).
 *   <li>{@code status} validated against {@link MilestoneStatus#ALL}; freely changeable (no state
 *       machine).
 *   <li>{@code projectId} immutable after create.
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class MilestoneService {

  private final MilestoneRepository repo;
  private final ProjectRepository projectRepo;

  public MilestoneService(MilestoneRepository repo, ProjectRepository projectRepo) {
    this.repo = repo;
    this.projectRepo = projectRepo;
  }

  @Transactional
  public MilestoneDetail create(MilestoneCreateRequest req) {
    if (!projectRepo.existsById(req.getProjectId())) {
      throw new BadRequestException("project not found: id=" + req.getProjectId());
    }
    if (repo.existsByProjectIdAndCode(req.getProjectId(), req.getCode())) {
      throw new ConflictException("code already exists: " + req.getCode());
    }
    String status = req.getStatus() == null ? MilestoneStatus.PLANNED : req.getStatus();
    if (!MilestoneStatus.ALL.contains(status)) {
      throw new BadRequestException("invalid status: " + status);
    }
    Milestone m = new Milestone();
    m.setProjectId(req.getProjectId());
    m.setCode(req.getCode());
    m.setName(req.getName());
    m.setDescription(req.getDescription());
    m.setTargetDate(req.getTargetDate());
    m.setStatus(status);
    m.setActualDate(req.getActualDate());
    m.setSortOrder(req.getSortOrder() == null ? 0 : req.getSortOrder());
    return MilestoneDetail.from(repo.saveAndFlush(m));
  }

  public MilestoneDetail findById(Long id) {
    return MilestoneDetail.from(getOrThrow(id));
  }

  public PageResponse<MilestoneDetail> list(Long projectId, String status, PageParams page) {
    Specification<Milestone> spec =
        (root, query, cb) -> {
          javax.persistence.criteria.Predicate p = cb.conjunction();
          if (projectId != null) {
            p = cb.and(p, cb.equal(root.get("projectId"), projectId));
          }
          if (status != null) {
            p = cb.and(p, cb.equal(root.get("status"), status));
          }
          String search = page.getSearch();
          if (search != null && !search.isEmpty()) {
            String pattern = "%" + search.toLowerCase() + "%";
            p =
                cb.and(
                    p,
                    cb.or(
                        cb.like(cb.lower(root.get("code")), pattern),
                        cb.like(cb.lower(root.get("name")), pattern)));
          }
          return p;
        };
    // D5: sortOrder ASC, then createTime DESC.
    PageRequest pr =
        PageRequest.of(
            page.getPage(),
            page.getSize(),
            Sort.by(Sort.Direction.ASC, "sortOrder")
                .and(Sort.by(Sort.Direction.DESC, "createTime")));
    Page<Milestone> result = repo.findAll(spec, pr);
    return PageResponse.of(
        result.stream().map(MilestoneDetail::from).collect(Collectors.toList()),
        page.getPage(),
        page.getSize(),
        result.getTotalElements());
  }

  @Transactional
  public MilestoneDetail update(Long id, MilestoneUpdateRequest req) {
    Milestone m = getOrThrow(id);
    if (!MilestoneStatus.ALL.contains(req.getStatus())) {
      throw new BadRequestException("invalid status: " + req.getStatus());
    }
    if (!req.getCode().equals(m.getCode())
        && repo.existsByProjectIdAndCode(m.getProjectId(), req.getCode())) {
      throw new ConflictException("code already exists: " + req.getCode());
    }
    m.setCode(req.getCode());
    m.setName(req.getName());
    m.setDescription(req.getDescription());
    m.setTargetDate(req.getTargetDate());
    m.setStatus(req.getStatus());
    m.setActualDate(req.getActualDate());
    // sortOrder absent → keep current (intentional keep-current; never cleared to null).
    if (req.getSortOrder() != null) {
      m.setSortOrder(req.getSortOrder());
    }
    // projectId immutable.
    return MilestoneDetail.from(repo.saveAndFlush(m));
  }

  @Transactional
  public void delete(Long id) {
    repo.delete(getOrThrow(id));
  }

  Milestone getOrThrow(Long id) {
    return repo.findById(id)
        .orElseThrow(() -> new NotFoundException("milestone not found: id=" + id));
  }
}
