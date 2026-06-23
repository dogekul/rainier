/* (C) 2026 Rainier — internal use only. */
package com.rainier.opportunity.controller;

import com.rainier.auth.controller.AuthController;
import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.opportunity.dto.OpportunityAdvanceRequest;
import com.rainier.opportunity.dto.OpportunityArtifactCreateRequest;
import com.rainier.opportunity.dto.OpportunityArtifactDetail;
import com.rainier.opportunity.dto.OpportunityCreateRequest;
import com.rainier.opportunity.dto.OpportunityDetail;
import com.rainier.opportunity.dto.OpportunityInitiateRequest;
import com.rainier.opportunity.dto.OpportunityUpdateRequest;
import com.rainier.opportunity.service.OpportunityArtifactService;
import com.rainier.opportunity.service.OpportunityService;
import java.net.URI;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 售前商机 endpoints (v0.0.44). all-users (token-gated). Stage advance + the 立项 handoff record the
 * caller as the gate decider.
 */
@RestController
@RequestMapping("/api/opportunities")
public class OpportunityController {

  private final OpportunityService service;
  private final OpportunityArtifactService artifactService;

  public OpportunityController(
      OpportunityService service, OpportunityArtifactService artifactService) {
    this.service = service;
    this.artifactService = artifactService;
  }

  @PostMapping
  public ResponseEntity<OpportunityDetail> create(@Valid @RequestBody OpportunityCreateRequest req) {
    OpportunityDetail created = service.create(req);
    return ResponseEntity.created(URI.create("/api/opportunities/" + created.getId())).body(created);
  }

  @GetMapping
  public PageResponse<OpportunityDetail> list(
      @RequestParam(required = false) String stage,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long ownerUserId,
      @Valid PageParams page) {
    return service.list(stage, status, ownerUserId, page);
  }

  @GetMapping("/{id}")
  public OpportunityDetail get(@PathVariable Long id) {
    return service.findById(id);
  }

  @PutMapping("/{id}")
  public OpportunityDetail update(
      @PathVariable Long id, @Valid @RequestBody OpportunityUpdateRequest req) {
    return service.update(id, req);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/advance")
  public OpportunityDetail advance(
      @PathVariable Long id,
      @Valid @RequestBody(required = false) OpportunityAdvanceRequest req,
      HttpServletRequest request) {
    String decision = req == null ? null : req.getDecision();
    String note = req == null ? null : req.getNote();
    return service.advance(
        id, decision, note, currentUser(request), req == null ? null : req.getArtifact());
  }

  /** v0.0.45 — list a 商机's 流转产出物 (append-only), most-recent first. */
  @GetMapping("/{id}/artifacts")
  public List<OpportunityArtifactDetail> artifacts(@PathVariable Long id) {
    return artifactService.list(id);
  }

  /** v0.0.45 — independently add a 流转产出物 (e.g. preparing POC deliverables before advancing). */
  @PostMapping("/{id}/artifacts")
  public ResponseEntity<OpportunityArtifactDetail> addArtifact(
      @PathVariable Long id, @Valid @RequestBody OpportunityArtifactCreateRequest req) {
    OpportunityArtifactDetail created = artifactService.create(id, req);
    return ResponseEntity.created(
            URI.create("/api/opportunities/" + id + "/artifacts/" + created.getId()))
        .body(created);
  }

  /** v0.0.45 — export one 产出物 as a Word .docx. */
  @GetMapping("/{id}/artifacts/{artifactId}/export")
  public ResponseEntity<byte[]> exportArtifact(
      @PathVariable Long id, @PathVariable Long artifactId) {
    byte[] docx = artifactService.export(id, artifactId);
    String filename = "artifact-" + id + "-" + artifactId + ".docx";
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
        .body(docx);
  }

  @PostMapping("/{id}/initiate")
  public OpportunityDetail initiate(
      @PathVariable Long id,
      @Valid @RequestBody OpportunityInitiateRequest req,
      HttpServletRequest request) {
    return service.initiate(id, req.getProjectId(), req.getDecision(), req.getNote(), currentUser(request));
  }

  private static String currentUser(HttpServletRequest request) {
    Object u = request.getAttribute(AuthController.ATTR_USERNAME);
    return (u instanceof String && !((String) u).isEmpty()) ? (String) u : "system";
  }
}
