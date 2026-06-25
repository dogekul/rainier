/* (C) 2026 Rainier — internal use only. */
package com.rainier.feature.service;

import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.ConflictException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.feature.domain.Feature;
import com.rainier.feature.domain.FeatureStatus;
import com.rainier.feature.dto.FeatureCreateRequest;
import com.rainier.feature.dto.FeatureDetail;
import com.rainier.feature.dto.FeatureUpdateRequest;
import com.rainier.feature.repository.FeatureRepository;
import com.rainier.productmodule.domain.ProductModule;
import com.rainier.productmodule.repository.ProductModuleRepository;
import com.rainier.requirementfeature.service.RequirementFeatureLinkService;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business operations for {@link Feature}. Leaf entity — no downstream FK protection.
 *
 * <ul>
 *   <li>{@code moduleId} immutable after creation.
 *   <li>{@code code} service-level unique.
 *   <li>Owner mutable.
 *   <li>Soft-deleted; can be directly removed without FK checks.
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class FeatureService {

  private final FeatureRepository repo;
  private final ProductModuleRepository moduleRepo;
  private final UserRepository userRepo;
  private final RequirementFeatureLinkService requirementFeatureLinkService;

  public FeatureService(
      FeatureRepository repo,
      ProductModuleRepository moduleRepo,
      UserRepository userRepo,
      RequirementFeatureLinkService requirementFeatureLinkService) {
    this.repo = repo;
    this.moduleRepo = moduleRepo;
    this.userRepo = userRepo;
    this.requirementFeatureLinkService = requirementFeatureLinkService;
  }

  @Transactional
  public FeatureDetail create(FeatureCreateRequest req) {
    if (!moduleRepo.existsById(req.getModuleId())) {
      throw new BadRequestException("module not found: id=" + req.getModuleId());
    }
    if (!userRepo.existsById(req.getOwnerUserId())) {
      throw new BadRequestException("owner user not found: id=" + req.getOwnerUserId());
    }
    if (repo.existsByCode(req.getCode())) {
      throw new ConflictException("code already exists: " + req.getCode());
    }
    String status = req.getStatus() == null ? FeatureStatus.PLANNING : req.getStatus();
    if (!FeatureStatus.ALL.contains(status)) {
      throw new BadRequestException("invalid status: " + status);
    }
    Feature f = new Feature();
    f.setCode(req.getCode());
    f.setName(req.getName());
    f.setDescription(req.getDescription());
    f.setStatus(status);
    f.setModuleId(req.getModuleId());
    f.setOwnerUserId(req.getOwnerUserId());
    return enrich(repo.saveAndFlush(f));
  }

  public FeatureDetail findById(Long id) {
    return enrich(getOrThrow(id));
  }

  public PageResponse<FeatureDetail> list(Long moduleId, String status, PageParams page) {
    Specification<Feature> spec =
        (root, query, cb) -> {
          javax.persistence.criteria.Predicate p = cb.conjunction();
          if (moduleId != null) {
            p = cb.and(p, cb.equal(root.get("moduleId"), moduleId));
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
    PageRequest pr =
        PageRequest.of(page.getPage(), page.getSize(), Sort.by(Sort.Direction.DESC, "createTime"));
    Page<Feature> result = repo.findAll(spec, pr);
    List<Feature> features = result.getContent();
    if (features.isEmpty()) {
      return PageResponse.of(
          Collections.emptyList(), page.getPage(), page.getSize(), result.getTotalElements());
    }
    Set<Long> userIds =
        features.stream()
            .map(Feature::getOwnerUserId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(HashSet::new));
    Set<Long> moduleIds =
        features.stream()
            .map(Feature::getModuleId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(HashSet::new));
    Map<Long, User> userMap =
        userIds.isEmpty() ? Collections.emptyMap() : batchUserById(userRepo.findAllById(userIds));
    Map<Long, ProductModule> moduleMap =
        moduleIds.isEmpty()
            ? Collections.emptyMap()
            : batchModuleById(moduleRepo.findAllById(moduleIds));
    Set<Long> featureIds =
        features.stream().map(Feature::getId).collect(Collectors.toCollection(HashSet::new));
    Map<Long, List<Long>> reqIdsMap =
        requirementFeatureLinkService.findRequirementIdsByFeatureIds(featureIds);
    return PageResponse.of(
        features.stream()
            .map(f -> enrichBatch(f, userMap, moduleMap, reqIdsMap))
            .collect(Collectors.toList()),
        page.getPage(),
        page.getSize(),
        result.getTotalElements());
  }

  @Transactional
  public FeatureDetail update(Long id, FeatureUpdateRequest req) {
    Feature f = getOrThrow(id);
    if (!FeatureStatus.ALL.contains(req.getStatus())) {
      throw new BadRequestException("invalid status: " + req.getStatus());
    }
    if (!Objects.equals(req.getOwnerUserId(), f.getOwnerUserId())) {
      if (!userRepo.existsById(req.getOwnerUserId())) {
        throw new BadRequestException("owner user not found: id=" + req.getOwnerUserId());
      }
      f.setOwnerUserId(req.getOwnerUserId());
    }
    if (!req.getCode().equals(f.getCode())) {
      if (repo.existsByCode(req.getCode())) {
        throw new ConflictException("code already exists: " + req.getCode());
      }
      f.setCode(req.getCode());
    }
    f.setName(req.getName());
    f.setDescription(req.getDescription());
    f.setStatus(req.getStatus());
    // moduleId intentionally NOT touched — immutable after creation.
    return enrich(repo.saveAndFlush(f));
  }

  @Transactional
  public void delete(Long id) {
    Feature f = getOrThrow(id);
    repo.delete(f);
  }

  Feature getOrThrow(Long id) {
    return repo.findById(id)
        .orElseThrow(() -> new NotFoundException("feature not found: id=" + id));
  }

  private FeatureDetail enrich(Feature f) {
    FeatureDetail dto = FeatureDetail.from(f);
    userRepo
        .findById(f.getOwnerUserId())
        .ifPresent(
            u -> {
              dto.setOwnerName(u.getName());
              dto.setOwnerLoginName(u.getLoginName());
            });
    moduleRepo
        .findById(f.getModuleId())
        .ifPresent(
            m -> {
              dto.setModuleCode(m.getCode());
              dto.setModuleName(m.getName());
            });
    dto.setRequirementIds(requirementFeatureLinkService.findRequirementIdsByFeature(f.getId()));
    return dto;
  }

  private static Map<Long, User> batchUserById(Iterable<User> users) {
    Map<Long, User> map = new HashMap<>();
    for (User u : users) {
      map.put(u.getId(), u);
    }
    return map;
  }

  private static Map<Long, ProductModule> batchModuleById(Iterable<ProductModule> modules) {
    Map<Long, ProductModule> map = new HashMap<>();
    for (ProductModule m : modules) {
      map.put(m.getId(), m);
    }
    return map;
  }

  private FeatureDetail enrichBatch(
      Feature f,
      Map<Long, User> userMap,
      Map<Long, ProductModule> moduleMap,
      Map<Long, List<Long>> reqIdsMap) {
    FeatureDetail dto = FeatureDetail.from(f);
    User u = userMap.get(f.getOwnerUserId());
    if (u != null) {
      dto.setOwnerName(u.getName());
      dto.setOwnerLoginName(u.getLoginName());
    }
    ProductModule m = moduleMap.get(f.getModuleId());
    if (m != null) {
      dto.setModuleCode(m.getCode());
      dto.setModuleName(m.getName());
    }
    List<Long> ids = reqIdsMap.get(f.getId());
    dto.setRequirementIds(ids == null ? Collections.<Long>emptyList() : ids);
    return dto;
  }
}
