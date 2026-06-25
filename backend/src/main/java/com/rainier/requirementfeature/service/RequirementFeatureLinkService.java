/* (C) 2026 Rainier — internal use only. */
package com.rainier.requirementfeature.service;

import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.ConflictException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.feature.repository.FeatureRepository;
import com.rainier.requirement.repository.RequirementRepository;
import com.rainier.requirementfeature.domain.RequirementFeatureLink;
import com.rainier.requirementfeature.dto.RequirementFeatureLinkCreateRequest;
import com.rainier.requirementfeature.dto.RequirementFeatureLinkDetail;
import com.rainier.requirementfeature.repository.RequirementFeatureLinkRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business operations for {@link RequirementFeatureLink}. Hard delete; no soft-delete column. */
@Service
@Transactional(readOnly = true)
public class RequirementFeatureLinkService {

  private final RequirementFeatureLinkRepository repo;
  private final RequirementRepository requirementRepo;
  private final FeatureRepository featureRepo;

  public RequirementFeatureLinkService(
      RequirementFeatureLinkRepository repo,
      RequirementRepository requirementRepo,
      FeatureRepository featureRepo) {
    this.repo = repo;
    this.requirementRepo = requirementRepo;
    this.featureRepo = featureRepo;
  }

  @Transactional
  public RequirementFeatureLinkDetail link(
      RequirementFeatureLinkCreateRequest req, Long currentUserId) {
    if (!requirementRepo.existsById(req.getRequirementId())) {
      throw new BadRequestException("requirement not found: id=" + req.getRequirementId());
    }
    if (!featureRepo.existsById(req.getFeatureId())) {
      throw new BadRequestException("feature not found: id=" + req.getFeatureId());
    }
    if (repo.existsByRequirementIdAndFeatureId(req.getRequirementId(), req.getFeatureId())) {
      throw new ConflictException("link already exists");
    }
    RequirementFeatureLink link = new RequirementFeatureLink();
    link.setRequirementId(req.getRequirementId());
    link.setFeatureId(req.getFeatureId());
    link.setLinkedAt(Instant.now());
    link.setLinkedByUserId(currentUserId);
    try {
      return RequirementFeatureLinkDetail.from(repo.saveAndFlush(link));
    } catch (DataIntegrityViolationException e) {
      throw new ConflictException("link already exists");
    }
  }

  @Transactional
  public void unlink(Long id) {
    RequirementFeatureLink link =
        repo.findById(id)
            .orElseThrow(() -> new NotFoundException("link not found: id=" + id));
    repo.delete(link);
  }

  public List<RequirementFeatureLinkDetail> listByRequirement(Long requirementId) {
    if (!requirementRepo.existsById(requirementId)) {
      throw new NotFoundException("requirement not found: id=" + requirementId);
    }
    return repo.findByRequirementId(requirementId).stream()
        .map(RequirementFeatureLinkDetail::from)
        .collect(Collectors.toList());
  }

  public List<RequirementFeatureLinkDetail> listByFeature(Long featureId) {
    if (!featureRepo.existsById(featureId)) {
      throw new NotFoundException("feature not found: id=" + featureId);
    }
    return repo.findByFeatureId(featureId).stream()
        .map(RequirementFeatureLinkDetail::from)
        .collect(Collectors.toList());
  }

  /** Used by RequirementService for enrichment. */
  public List<Long> findFeatureIdsByRequirement(Long requirementId) {
    List<RequirementFeatureLink> links = repo.findByRequirementId(requirementId);
    if (links.isEmpty()) return Collections.emptyList();
    List<Long> ids = new ArrayList<Long>(links.size());
    Set<Long> seen = new HashSet<Long>();
    for (RequirementFeatureLink l : links) {
      if (seen.add(l.getFeatureId())) ids.add(l.getFeatureId());
    }
    return ids;
  }

  /** Used by FeatureService for enrichment. */
  public List<Long> findRequirementIdsByFeature(Long featureId) {
    List<RequirementFeatureLink> links = repo.findByFeatureId(featureId);
    if (links.isEmpty()) return Collections.emptyList();
    List<Long> ids = new ArrayList<Long>(links.size());
    Set<Long> seen = new HashSet<Long>();
    for (RequirementFeatureLink l : links) {
      if (seen.add(l.getRequirementId())) ids.add(l.getRequirementId());
    }
    return ids;
  }

  /** Batch enrichment: requirementId -> [featureIds]. */
  public java.util.Map<Long, List<Long>> findFeatureIdsByRequirementIds(
      Collection<Long> requirementIds) {
    if (requirementIds == null || requirementIds.isEmpty()) return Collections.emptyMap();
    java.util.Map<Long, List<Long>> result = new java.util.HashMap<Long, List<Long>>();
    for (RequirementFeatureLink l : repo.findByRequirementIdIn(requirementIds)) {
      List<Long> bucket = result.get(l.getRequirementId());
      if (bucket == null) {
        bucket = new ArrayList<Long>();
        result.put(l.getRequirementId(), bucket);
      }
      if (!bucket.contains(l.getFeatureId())) bucket.add(l.getFeatureId());
    }
    return result;
  }

  /** Batch enrichment: featureId -> [requirementIds]. */
  public java.util.Map<Long, List<Long>> findRequirementIdsByFeatureIds(
      Collection<Long> featureIds) {
    if (featureIds == null || featureIds.isEmpty()) return Collections.emptyMap();
    java.util.Map<Long, List<Long>> result = new java.util.HashMap<Long, List<Long>>();
    for (RequirementFeatureLink l : repo.findByFeatureIdIn(featureIds)) {
      List<Long> bucket = result.get(l.getFeatureId());
      if (bucket == null) {
        bucket = new ArrayList<Long>();
        result.put(l.getFeatureId(), bucket);
      }
      if (!bucket.contains(l.getRequirementId())) bucket.add(l.getRequirementId());
    }
    return result;
  }
}
