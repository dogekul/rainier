/* (C) 2026 Rainier — internal use only. */
package com.rainier.opportunity.service;

import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.opportunity.domain.ArtifactType;
import com.rainier.opportunity.domain.Opportunity;
import com.rainier.opportunity.domain.OpportunityArtifact;
import com.rainier.opportunity.dto.OpportunityArtifactCreateRequest;
import com.rainier.opportunity.dto.OpportunityArtifactDetail;
import com.rainier.opportunity.repository.OpportunityArtifactRepository;
import com.rainier.opportunity.repository.OpportunityRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read + export side of 流转产出物 (v0.0.45). Creation happens only inside the advance gate
 * ({@link OpportunityService#advance}); this service lists them and renders Word exports.
 */
@Service
@Transactional(readOnly = true)
public class OpportunityArtifactService {

  private final OpportunityArtifactRepository repo;
  private final OpportunityRepository opportunityRepo;

  public OpportunityArtifactService(
      OpportunityArtifactRepository repo, OpportunityRepository opportunityRepo) {
    this.repo = repo;
    this.opportunityRepo = opportunityRepo;
  }

  /** Independently add a 流转产出物 (v0.0.45) — e.g. preparing POC deliverables before advancing. */
  @Transactional
  public OpportunityArtifactDetail create(Long opportunityId, OpportunityArtifactCreateRequest req) {
    if (!opportunityRepo.existsById(opportunityId)) {
      throw new NotFoundException("opportunity not found: id=" + opportunityId);
    }
    if (!ArtifactType.ALL.contains(req.getType())) {
      throw new BadRequestException("unknown artifact type: " + req.getType());
    }
    boolean blankContent = req.getContent() == null || req.getContent().trim().isEmpty();
    boolean blankLink = req.getLink() == null || req.getLink().trim().isEmpty();
    boolean link = ArtifactType.isLink(req.getType());
    if (link && blankLink) {
      throw new BadRequestException("链接类产出物需填写链接");
    }
    if (!link && blankContent) {
      throw new BadRequestException("报告类产出物需填写正文");
    }
    OpportunityArtifact a = new OpportunityArtifact();
    a.setOpportunityId(opportunityId);
    a.setType(req.getType());
    // title is optional (链接类材料无需标题) — default to the type label so the column stays non-null.
    boolean blankTitle = req.getTitle() == null || req.getTitle().trim().isEmpty();
    a.setTitle(blankTitle ? ArtifactType.label(req.getType()) : req.getTitle().trim());
    // store only the relevant field for the kind (link-kind → link; report-kind → content)
    a.setContent(link ? null : req.getContent());
    a.setLink(link ? req.getLink().trim() : null);
    return OpportunityArtifactDetail.from(repo.saveAndFlush(a));
  }

  public List<OpportunityArtifactDetail> list(Long opportunityId) {
    if (!opportunityRepo.existsById(opportunityId)) {
      throw new NotFoundException("opportunity not found: id=" + opportunityId);
    }
    return repo.findByOpportunityIdOrderByIdDesc(opportunityId).stream()
        .map(OpportunityArtifactDetail::from)
        .collect(Collectors.toList());
  }

  /** Render one artifact to a .docx byte array. 404 if the artifact does not belong to the opportunity. */
  public byte[] export(Long opportunityId, Long artifactId) {
    OpportunityArtifact a =
        repo.findById(artifactId)
            .orElseThrow(() -> new NotFoundException("artifact not found: id=" + artifactId));
    if (!a.getOpportunityId().equals(opportunityId)) {
      throw new NotFoundException(
          "artifact " + artifactId + " does not belong to opportunity " + opportunityId);
    }
    Opportunity o = opportunityRepo.findById(opportunityId).orElse(null);
    return DocxWriter.write(a, o);
  }
}
