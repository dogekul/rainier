/* (C) 2026 Rainier — internal use only. */
package com.rainier.demandrequirement.controller;

import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.demandrequirement.dto.DemandRequirementLinkCreateRequest;
import com.rainier.demandrequirement.dto.DemandRequirementLinkDetail;
import com.rainier.demandrequirement.service.DemandRequirementLinkService;
import java.net.URI;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoints for {@link com.rainier.demandrequirement.domain.DemandRequirementLink}. */
@RestController
@RequestMapping("/api/demand-requirements")
public class DemandRequirementLinkController {

  private final DemandRequirementLinkService service;

  public DemandRequirementLinkController(DemandRequirementLinkService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<DemandRequirementLinkDetail> create(
      @Valid @RequestBody DemandRequirementLinkCreateRequest req) {
    DemandRequirementLinkDetail created = service.create(req);
    return ResponseEntity.created(URI.create("/api/demand-requirements/" + created.getId()))
        .body(created);
  }

  @GetMapping("/{id}")
  public DemandRequirementLinkDetail get(@PathVariable Long id) {
    return service.findById(id);
  }

  @GetMapping
  public PageResponse<DemandRequirementLinkDetail> list(
      @RequestParam(required = false) Long demandId,
      @RequestParam(required = false) Long requirementId,
      @Valid PageParams page) {
    return service.list(demandId, requirementId, page);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
