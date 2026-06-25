/* (C) 2026 Rainier — internal use only. */
package com.rainier.opportunity.controller;

import com.rainier.opportunity.dto.StageActivityCreateRequest;
import com.rainier.opportunity.dto.StageActivityDetail;
import com.rainier.opportunity.dto.StageDashboardView;
import com.rainier.opportunity.service.StageActivityService;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * D2 (v0.0.90) — 商机各 stage 的「活动清单」+「dashboard 整合视图」endpoints. token-optional, all users.
 */
@RestController
public class StageActivityController {

  private final StageActivityService service;

  public StageActivityController(StageActivityService service) {
    this.service = service;
  }

  @GetMapping("/api/opportunities/{id}/stages/{code}/activities")
  public List<StageActivityDetail> list(@PathVariable Long id, @PathVariable String code) {
    return service.listByOpportunityAndStage(id, code);
  }

  @PostMapping("/api/opportunities/{id}/stages/{code}/activities")
  public ResponseEntity<StageActivityDetail> add(
      @PathVariable Long id,
      @PathVariable String code,
      @RequestBody(required = false) StageActivityCreateRequest req) {
    StageActivityDetail created = service.addActivity(id, code, req);
    return ResponseEntity.created(
            URI.create(
                "/api/opportunities/" + id + "/stages/" + code + "/activities/" + created.getId()))
        .body(created);
  }

  @PostMapping("/api/stage-activities/{aid}/done")
  public StageActivityDetail done(@PathVariable Long aid) {
    return service.markDone(aid);
  }

  @PostMapping("/api/stage-activities/{aid}/skip")
  public StageActivityDetail skip(@PathVariable Long aid) {
    return service.skip(aid);
  }

  @GetMapping("/api/opportunities/{id}/stages/{code}/dashboard")
  public StageDashboardView dashboard(@PathVariable Long id, @PathVariable String code) {
    return service.dashboard(id, code);
  }
}
