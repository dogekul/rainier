/* (C) 2026 Rainier — internal use only. */
package com.rainier.requirementfeature.controller;

import com.rainier.common.authz.AuthzService;
import com.rainier.requirementfeature.dto.RequirementFeatureLinkCreateRequest;
import com.rainier.requirementfeature.dto.RequirementFeatureLinkDetail;
import com.rainier.requirementfeature.service.RequirementFeatureLinkService;
import java.net.URI;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoints for {@link com.rainier.requirementfeature.domain.RequirementFeatureLink}. */
@RestController
@RequestMapping("/api/requirement-features")
public class RequirementFeatureLinkController {

  private final RequirementFeatureLinkService service;
  private final AuthzService authz;

  public RequirementFeatureLinkController(
      RequirementFeatureLinkService service, AuthzService authz) {
    this.service = service;
    this.authz = authz;
  }

  @PostMapping
  public ResponseEntity<RequirementFeatureLinkDetail> create(
      @Valid @RequestBody RequirementFeatureLinkCreateRequest req, HttpServletRequest http) {
    Long uid = authz.currentUserId(http);
    RequirementFeatureLinkDetail created = service.link(req, uid);
    return ResponseEntity.created(URI.create("/api/requirement-features/" + created.getId()))
        .body(created);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.unlink(id);
    return ResponseEntity.noContent().build();
  }

  /**
   * Auxiliary read endpoints under the existing resource paths:
   *
   * <ul>
   *   <li>{@code GET /api/requirements/{id}/linked-features} → list this requirement's links
   *   <li>{@code GET /api/features/{id}/linked-requirements} → list this feature's links
   * </ul>
   *
   * <p>The {@code /requirements/{id}/features} path was already taken (sprint-feature 2-hop, see
   * {@link com.rainier.requirement.controller.RequirementController#getFeatures}), so the direct
   * links use {@code linked-features} / {@code linked-requirements} to avoid breaking callers.
   */
  @org.springframework.web.bind.annotation.GetMapping("/by-requirement/{requirementId}")
  public List<RequirementFeatureLinkDetail> byRequirement(@PathVariable Long requirementId) {
    return service.listByRequirement(requirementId);
  }

  @org.springframework.web.bind.annotation.GetMapping("/by-feature/{featureId}")
  public List<RequirementFeatureLinkDetail> byFeature(@PathVariable Long featureId) {
    return service.listByFeature(featureId);
  }
}
