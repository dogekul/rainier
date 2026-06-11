/* (C) 2026 Rainier — internal use only. */
package com.rainier.sprintfeature.service;

import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.ConflictException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.feature.domain.Feature;
import com.rainier.feature.repository.FeatureRepository;
import com.rainier.productmodule.domain.ProductModule;
import com.rainier.productmodule.repository.ProductModuleRepository;
import com.rainier.requirement.repository.RequirementRepository;
import com.rainier.sprint.domain.Sprint;
import com.rainier.sprint.repository.SprintRepository;
import com.rainier.sprintfeature.domain.SprintFeatureLink;
import com.rainier.sprintfeature.dto.FeatureSprintView;
import com.rainier.sprintfeature.dto.SprintFeatureLinkCreateRequest;
import com.rainier.sprintfeature.dto.SprintFeatureLinkDetail;
import com.rainier.sprintfeature.dto.SprintFeatureView;
import com.rainier.sprintfeature.repository.SprintFeatureLinkRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business operations for {@link SprintFeatureLink}. Bridges a Sprint (产品迭代) to a Feature.
 *
 * <ul>
 *   <li>Hard delete; no soft-delete column.
 *   <li>Product consistency: a feature's product (feature → module → product) must equal the
 *       sprint's productId.
 *   <li>{@code sprint.productId} is lazily established by the first linked feature and immutable
 *       once non-null (design Decision 2/3).
 * </ul>
 *
 * <p>Depends only on repositories (not on Sprint/Feature business services) so the reverse-query
 * endpoints can inject it into Sprint/Feature/Requirement controllers without a cycle.
 */
@Service
@Transactional(readOnly = true)
public class SprintFeatureLinkService {

  private final SprintFeatureLinkRepository repo;
  private final SprintRepository sprintRepo;
  private final FeatureRepository featureRepo;
  private final ProductModuleRepository moduleRepo;
  private final RequirementRepository requirementRepo;

  public SprintFeatureLinkService(
      SprintFeatureLinkRepository repo,
      SprintRepository sprintRepo,
      FeatureRepository featureRepo,
      ProductModuleRepository moduleRepo,
      RequirementRepository requirementRepo) {
    this.repo = repo;
    this.sprintRepo = sprintRepo;
    this.featureRepo = featureRepo;
    this.moduleRepo = moduleRepo;
    this.requirementRepo = requirementRepo;
  }

  @Transactional
  public SprintFeatureLinkDetail create(SprintFeatureLinkCreateRequest req) {
    // Validation order (design Decision 7): sprint → feature → product consistency → uniqueness.
    Sprint sprint =
        sprintRepo
            .findById(req.getSprintId())
            .orElseThrow(() -> new BadRequestException("sprint not found: id=" + req.getSprintId()));
    Feature feature =
        featureRepo
            .findById(req.getFeatureId())
            .orElseThrow(
                () -> new BadRequestException("feature not found: id=" + req.getFeatureId()));
    Long featureProductId = resolveFeatureProductId(feature);
    if (sprint.getProductId() == null) {
      // Decision 2: lazy establish — first feature fixes the sprint's product (same transaction).
      sprint.setProductId(featureProductId);
      sprintRepo.saveAndFlush(sprint);
    } else if (!sprint.getProductId().equals(featureProductId)) {
      throw new BadRequestException("feature must belong to the sprint's product");
    }
    if (repo.existsBySprintIdAndFeatureId(req.getSprintId(), req.getFeatureId())) {
      throw new ConflictException("link already exists");
    }
    SprintFeatureLink link = new SprintFeatureLink();
    link.setSprintId(req.getSprintId());
    link.setFeatureId(req.getFeatureId());
    try {
      return SprintFeatureLinkDetail.from(repo.saveAndFlush(link));
    } catch (DataIntegrityViolationException e) {
      // Concurrent insert race: existsBy returned false but DB unique constraint rejected the row.
      throw new ConflictException("link already exists");
    }
  }

  public SprintFeatureLinkDetail findById(Long id) {
    return SprintFeatureLinkDetail.from(getOrThrow(id));
  }

  public PageResponse<SprintFeatureLinkDetail> list(
      Long sprintId, Long featureId, PageParams page) {
    Specification<SprintFeatureLink> spec =
        (root, query, cb) -> {
          javax.persistence.criteria.Predicate p = cb.conjunction();
          if (sprintId != null) {
            p = cb.and(p, cb.equal(root.get("sprintId"), sprintId));
          }
          if (featureId != null) {
            p = cb.and(p, cb.equal(root.get("featureId"), featureId));
          }
          return p;
        };
    PageRequest pr =
        PageRequest.of(page.getPage(), page.getSize(), Sort.by(Sort.Direction.DESC, "createTime"));
    Page<SprintFeatureLink> result = repo.findAll(spec, pr);
    return PageResponse.of(
        result.stream().map(SprintFeatureLinkDetail::from).collect(Collectors.toList()),
        page.getPage(),
        page.getSize(),
        result.getTotalElements());
  }

  @Transactional
  public void delete(Long id) {
    repo.delete(getOrThrow(id));
  }

  /** Reverse query — features linked to a sprint (for GET /api/sprints/{id}/features). */
  public List<SprintFeatureView> findFeaturesBySprint(Long sprintId) {
    if (!sprintRepo.existsById(sprintId)) {
      throw new NotFoundException("sprint not found: id=" + sprintId);
    }
    List<SprintFeatureLink> links = repo.findBySprintId(sprintId);
    List<SprintFeatureView> out = new ArrayList<>(links.size());
    for (SprintFeatureLink link : links) {
      featureRepo
          .findById(link.getFeatureId())
          .ifPresent(f -> out.add(SprintFeatureView.from(f, link)));
    }
    return out;
  }

  /** Reverse query — sprints a feature is linked into (for GET /api/features/{id}/sprints). */
  public List<FeatureSprintView> findSprintsByFeature(Long featureId) {
    if (!featureRepo.existsById(featureId)) {
      throw new NotFoundException("feature not found: id=" + featureId);
    }
    List<SprintFeatureLink> links = repo.findByFeatureId(featureId);
    List<FeatureSprintView> out = new ArrayList<>(links.size());
    for (SprintFeatureLink link : links) {
      sprintRepo
          .findById(link.getSprintId())
          .ifPresent(s -> out.add(FeatureSprintView.from(s, link)));
    }
    return out;
  }

  /**
   * Reverse query — features reached by a requirement through all its sprints (2-hop, deduped,
   * order-preserving). For GET /api/requirements/{id}/features.
   */
  public List<SprintFeatureView> findFeaturesByRequirement(Long requirementId) {
    if (!requirementRepo.existsById(requirementId)) {
      throw new NotFoundException("requirement not found: id=" + requirementId);
    }
    List<Sprint> sprints = sprintRepo.findByRequirementId(requirementId);
    // Keep one link per featureId (LinkedHashSet preserves first-seen order; design Decision 9).
    Map<Long, SprintFeatureLink> firstLinkByFeature = new HashMap<>();
    Set<Long> featureIds = new LinkedHashSet<>();
    for (Sprint s : sprints) {
      for (SprintFeatureLink link : repo.findBySprintId(s.getId())) {
        if (featureIds.add(link.getFeatureId())) {
          firstLinkByFeature.put(link.getFeatureId(), link);
        }
      }
    }
    if (featureIds.isEmpty()) {
      return new ArrayList<>();
    }
    Map<Long, Feature> featureMap = new HashMap<>();
    for (Feature f : featureRepo.findAllById(featureIds)) {
      featureMap.put(f.getId(), f);
    }
    List<SprintFeatureView> out = new ArrayList<>(featureIds.size());
    for (Long fid : featureIds) {
      Feature f = featureMap.get(fid);
      if (f != null) {
        out.add(SprintFeatureView.from(f, firstLinkByFeature.get(fid)));
      }
    }
    return out;
  }

  SprintFeatureLink getOrThrow(Long id) {
    return repo.findById(id).orElseThrow(() -> new NotFoundException("link not found: id=" + id));
  }

  /** Resolves a feature's product via feature → module → product (2-hop). */
  private Long resolveFeatureProductId(Feature feature) {
    ProductModule module =
        moduleRepo
            .findById(feature.getModuleId())
            .orElseThrow(
                () ->
                    new BadRequestException(
                        "module not found for feature: moduleId=" + feature.getModuleId()));
    return module.getProductId();
  }
}
