/* (C) 2026 Rainier — internal use only. */
package com.rainier.demand.controller;

import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.demand.dto.DemandCreateRequest;
import com.rainier.demand.dto.DemandDetail;
import com.rainier.demand.dto.DemandUpdateRequest;
import com.rainier.demand.service.DemandService;
import com.rainier.demandrequirement.dto.DerivedRequirementView;
import com.rainier.demandrequirement.service.DemandRequirementLinkService;
import java.net.URI;
import java.util.List;
import javax.validation.Valid;
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

/** REST endpoints for {@link com.rainier.demand.domain.Demand}. */
@RestController
@RequestMapping("/api/demands")
public class DemandController {

  private final DemandService service;
  private final DemandRequirementLinkService linkService;

  public DemandController(DemandService service, DemandRequirementLinkService linkService) {
    this.service = service;
    this.linkService = linkService;
  }

  @PostMapping
  public ResponseEntity<DemandDetail> create(@Valid @RequestBody DemandCreateRequest req) {
    DemandDetail created = service.create(req);
    return ResponseEntity.created(URI.create("/api/demands/" + created.getId())).body(created);
  }

  @GetMapping("/{id}")
  public DemandDetail get(@PathVariable Long id) {
    return service.findById(id);
  }

  @GetMapping
  public PageResponse<DemandDetail> list(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String priority,
      @RequestParam(required = false) Long opportunityId,
      @Valid PageParams page) {
    return service.list(status, priority, opportunityId, page);
  }

  @PutMapping("/{id}")
  public DemandDetail update(@PathVariable Long id, @Valid @RequestBody DemandUpdateRequest req) {
    return service.update(id, req);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }

  /** Auxiliary: list requirements derived from this demand (TC-DRL-007). */
  @GetMapping("/{id}/derived-requirements")
  public List<DerivedRequirementView> getDerivedRequirements(@PathVariable Long id) {
    // The service exists only after M04 wires the link service; method below is added there.
    return linkService.findDerivedRequirements(id);
  }
}
