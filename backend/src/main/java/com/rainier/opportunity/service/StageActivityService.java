/* (C) 2026 Rainier — internal use only. */
package com.rainier.opportunity.service;

import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.opportunity.domain.OpportunityStage;
import com.rainier.opportunity.domain.StageActivity;
import com.rainier.opportunity.dto.OpportunityArtifactDetail;
import com.rainier.opportunity.dto.StageActivityCreateRequest;
import com.rainier.opportunity.dto.StageActivityDetail;
import com.rainier.opportunity.dto.StageDashboardView;
import com.rainier.opportunity.repository.OpportunityRepository;
import com.rainier.opportunity.repository.StageActivityRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * D2 (v0.0.90) — 商机各 stage 下的「活动清单」CRUD + dashboard 整合（活动 + 该 stage 产出物）。
 * 活动是过程动作，无门禁意义；与 OpportunityArtifact (产出物=结果) 并列展示。
 */
@Service
@Transactional(readOnly = true)
public class StageActivityService {

  private final StageActivityRepository repo;
  private final OpportunityRepository opportunityRepo;
  private final OpportunityArtifactService artifactService;

  public StageActivityService(
      StageActivityRepository repo,
      OpportunityRepository opportunityRepo,
      OpportunityArtifactService artifactService) {
    this.repo = repo;
    this.opportunityRepo = opportunityRepo;
    this.artifactService = artifactService;
  }

  public List<StageActivityDetail> listByOpportunityAndStage(Long opportunityId, String stageCode) {
    requireOpportunity(opportunityId);
    requireStage(stageCode);
    return repo.findByOpportunityIdAndStageCodeOrderByIdAsc(opportunityId, stageCode).stream()
        .map(StageActivityDetail::from)
        .collect(Collectors.toList());
  }

  @Transactional
  public StageActivityDetail addActivity(
      Long opportunityId, String stageCode, StageActivityCreateRequest req) {
    requireOpportunity(opportunityId);
    requireStage(stageCode);
    if (req == null
        || req.getActivityTitle() == null
        || req.getActivityTitle().trim().isEmpty()) {
      throw new BadRequestException("活动标题必填");
    }
    StageActivity a = new StageActivity();
    a.setOpportunityId(opportunityId);
    a.setStageCode(stageCode);
    a.setActivityTitle(req.getActivityTitle().trim());
    a.setDescription(req.getDescription());
    a.setAssigneeUserId(req.getAssigneeUserId());
    a.setDueDate(req.getDueDate());
    a.setStatus(StageActivity.STATUS_PENDING);
    return StageActivityDetail.from(repo.saveAndFlush(a));
  }

  @Transactional
  public StageActivityDetail markDone(Long activityId) {
    StageActivity a = requireActivity(activityId);
    requirePending(a, "完成");
    a.setStatus(StageActivity.STATUS_DONE);
    a.setCompletedAt(Instant.now());
    return StageActivityDetail.from(repo.saveAndFlush(a));
  }

  @Transactional
  public StageActivityDetail skip(Long activityId) {
    StageActivity a = requireActivity(activityId);
    requirePending(a, "跳过");
    a.setStatus(StageActivity.STATUS_SKIPPED);
    a.setCompletedAt(null);
    return StageActivityDetail.from(repo.saveAndFlush(a));
  }

  public StageDashboardView dashboard(Long opportunityId, String stageCode) {
    requireOpportunity(opportunityId);
    requireStage(stageCode);
    List<StageActivityDetail> activities =
        repo.findByOpportunityIdAndStageCodeOrderByIdAsc(opportunityId, stageCode).stream()
            .map(StageActivityDetail::from)
            .collect(Collectors.toList());
    List<OpportunityArtifactDetail> all = artifactService.list(opportunityId);
    List<OpportunityArtifactDetail> stageArtifacts = new ArrayList<OpportunityArtifactDetail>();
    for (OpportunityArtifactDetail art : all) {
      if (stageCode.equals(art.getStageFrom())) {
        stageArtifacts.add(art);
      }
    }
    return new StageDashboardView(opportunityId, stageCode, activities, stageArtifacts);
  }

  private void requireOpportunity(Long opportunityId) {
    if (opportunityId == null || !opportunityRepo.existsById(opportunityId)) {
      throw new NotFoundException("opportunity not found: id=" + opportunityId);
    }
  }

  private void requireStage(String stageCode) {
    if (stageCode == null || !OpportunityStage.ALL.contains(stageCode)) {
      throw new BadRequestException("unknown stage code: " + stageCode);
    }
  }

  private StageActivity requireActivity(Long id) {
    return repo.findById(id)
        .orElseThrow(
            new java.util.function.Supplier<NotFoundException>() {
              @Override
              public NotFoundException get() {
                return new NotFoundException("stage activity not found: id=" + id);
              }
            });
  }

  private void requirePending(StageActivity a, String action) {
    if (!StageActivity.STATUS_PENDING.equals(a.getStatus())) {
      throw new BadRequestException("活动已是终态 (" + a.getStatus() + ")，不能" + action);
    }
  }
}
