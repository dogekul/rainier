/* (C) 2026 Rainier — internal use only. */
package com.rainier.position.controller;

import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.position.dto.PositionCreateRequest;
import com.rainier.position.dto.PositionDetail;
import com.rainier.position.dto.PositionUpdateRequest;
import com.rainier.position.service.PositionService;
import java.net.URI;
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

/** REST endpoints for {@link com.rainier.position.domain.Position}. */
@RestController
@RequestMapping("/api/positions")
public class PositionController {

  private final PositionService service;

  public PositionController(PositionService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<PositionDetail> create(@Valid @RequestBody PositionCreateRequest req) {
    PositionDetail created = service.create(req);
    return ResponseEntity.created(URI.create("/api/positions/" + created.getId())).body(created);
  }

  @GetMapping("/{id}")
  public PositionDetail get(@PathVariable Long id) {
    return service.findById(id);
  }

  @GetMapping
  public PageResponse<PositionDetail> list(
      @RequestParam(required = false) String category,
      @RequestParam(required = false) Boolean enabled,
      @Valid PageParams page) {
    return service.list(category, enabled, page);
  }

  @PutMapping("/{id}")
  public PositionDetail update(
      @PathVariable Long id, @Valid @RequestBody PositionUpdateRequest req) {
    return service.update(id, req);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
