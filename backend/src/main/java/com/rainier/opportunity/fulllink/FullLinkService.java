/* (C) 2026 Rainier — internal use only. */
package com.rainier.opportunity.fulllink;

import com.rainier.common.exception.NotFoundException;
import com.rainier.customer.dto.CustomerDetail;
import com.rainier.customer.repository.CustomerRepository;
import com.rainier.operation.dto.OperationDetail;
import com.rainier.operation.service.OperationService;
import com.rainier.opportunity.domain.Opportunity;
import com.rainier.opportunity.domain.OpportunityStage;
import com.rainier.opportunity.domain.StageActivity;
import com.rainier.opportunity.dto.OpportunityDetail;
import com.rainier.opportunity.repository.OpportunityArtifactRepository;
import com.rainier.opportunity.repository.OpportunityRepository;
import com.rainier.opportunity.repository.StageActivityRepository;
import com.rainier.opportunity.service.OpportunityService;
import com.rainier.operation.repository.OperationRepository;
import com.rainier.project.dto.ProjectDetail;
import com.rainier.project.service.ProjectService;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Build the 全链 (商机 → 项目 → 运营) read model for one opportunity (v0.0.94 D6).
 *
 * <p>Read-only aggregation; reuses existing service-level enrichments (OpportunityService /
 * ProjectService / OperationService) so name joins stay consistent with their respective detail
 * pages.
 */
@Service
@Transactional(readOnly = true)
public class FullLinkService {

  private static final List<String> PRESALE =
      Arrays.asList(
          OpportunityStage.LEAD,
          OpportunityStage.OPPORTUNITY,
          OpportunityStage.POC,
          OpportunityStage.BIDDING,
          OpportunityStage.CONTRACT);
  private static final List<String> DELIVERY =
      Arrays.asList(
          OpportunityStage.INITIATION,
          OpportunityStage.SURVEY,
          OpportunityStage.REQUIREMENT,
          OpportunityStage.DELIVERY,
          OpportunityStage.ACCEPTANCE);

  private static final Map<String, String> LABELS;

  static {
    LABELS = new LinkedHashMap<String, String>();
    LABELS.put(OpportunityStage.LEAD, "线索");
    LABELS.put(OpportunityStage.OPPORTUNITY, "商机");
    LABELS.put(OpportunityStage.POC, "推介/POC");
    LABELS.put(OpportunityStage.BIDDING, "投标");
    LABELS.put(OpportunityStage.CONTRACT, "合同签订");
    LABELS.put(OpportunityStage.INITIATION, "立项");
    LABELS.put(OpportunityStage.SURVEY, "现场调研");
    LABELS.put(OpportunityStage.REQUIREMENT, "产品诉求");
    LABELS.put(OpportunityStage.DELIVERY, "交付实施");
    LABELS.put(OpportunityStage.ACCEPTANCE, "验收");
  }

  private final OpportunityRepository opportunityRepo;
  private final OpportunityService opportunityService;
  private final ProjectService projectService;
  private final OperationService operationService;
  private final OperationRepository operationRepo;
  private final CustomerRepository customerRepo;
  private final StageActivityRepository stageActivityRepo;
  private final OpportunityArtifactRepository artifactRepo;

  public FullLinkService(
      OpportunityRepository opportunityRepo,
      OpportunityService opportunityService,
      ProjectService projectService,
      OperationService operationService,
      OperationRepository operationRepo,
      CustomerRepository customerRepo,
      StageActivityRepository stageActivityRepo,
      OpportunityArtifactRepository artifactRepo) {
    this.opportunityRepo = opportunityRepo;
    this.opportunityService = opportunityService;
    this.projectService = projectService;
    this.operationService = operationService;
    this.operationRepo = operationRepo;
    this.customerRepo = customerRepo;
    this.stageActivityRepo = stageActivityRepo;
    this.artifactRepo = artifactRepo;
  }

  public FullLinkResponse buildFor(Long opportunityId) {
    Opportunity opp =
        opportunityRepo
            .findById(opportunityId)
            .orElseThrow(
                () -> new NotFoundException("opportunity not found: id=" + opportunityId));

    FullLinkResponse out = new FullLinkResponse();
    OpportunityDetail oppDetail = opportunityService.findById(opp.getId());
    out.setOpportunity(oppDetail);

    if (opp.getCustomerId() != null) {
      customerRepo
          .findById(opp.getCustomerId())
          .ifPresent(c -> out.setCustomer(CustomerDetail.from(c)));
    }

    if (opp.getProjectId() != null) {
      try {
        ProjectDetail pd = projectService.findById(opp.getProjectId());
        out.setProject(pd);
      } catch (NotFoundException ignore) {
        // project soft-deleted — leave null
      }
    }

    operationRepo
        .findFirstByOpportunityId(opp.getId())
        .ifPresent(
            op -> {
              try {
                OperationDetail od = operationService.findById(op.getId());
                out.setOperation(od);
              } catch (NotFoundException ignore) {
                // operation removed — leave null
              }
            });

    String current = opp.getStage();
    out.setPresaleStages(buildStages(opp.getId(), PRESALE, current));
    out.setDeliveryStages(buildStages(opp.getId(), DELIVERY, current));
    return out;
  }

  private List<StageSummary> buildStages(Long opportunityId, List<String> codes, String current) {
    java.util.List<StageSummary> stages = new java.util.ArrayList<StageSummary>(codes.size());
    int currentIdx = indexOf(current);
    // artifact total at opportunity (we don't have a stage column on artifact); for the「current」
    // stage we surface the total — earlier stages get 0 (they were completed pre-counter).
    int artifactTotal = artifactRepo.findByOpportunityIdOrderByIdDesc(opportunityId).size();
    for (String code : codes) {
      List<StageActivity> acts =
          stageActivityRepo.findByOpportunityIdAndStageCodeOrderByIdAsc(opportunityId, code);
      int done = 0;
      for (StageActivity a : acts) {
        if (StageActivity.STATUS_DONE.equals(a.getStatus())) {
          done++;
        }
      }
      boolean isCurrent = code.equals(current);
      int artifactCount = isCurrent ? artifactTotal : 0;
      stages.add(
          new StageSummary(code, LABELS.get(code), isCurrent, acts.size(), done, artifactCount));
      // mark all stages strictly before current as fully-done implicitly via UI; counts above stay
      // honest (only StageActivity rows actually recorded)
      if (currentIdx < 0) {
        // current not in our chain — nothing extra
      }
    }
    return stages;
  }

  private static int indexOf(String code) {
    int i = PRESALE.indexOf(code);
    if (i >= 0) {
      return i;
    }
    int j = DELIVERY.indexOf(code);
    if (j >= 0) {
      return PRESALE.size() + j;
    }
    return -1;
  }
}
