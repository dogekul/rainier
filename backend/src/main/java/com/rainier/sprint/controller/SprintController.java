/* (C) 2026 Rainier — internal use only. */
package com.rainier.sprint.controller;

import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.sprint.dto.SprintCreateRequest;
import com.rainier.sprint.dto.SprintDetail;
import com.rainier.sprint.dto.SprintUpdateRequest;
import com.rainier.sprint.service.SprintService;
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

/** REST endpoints for {@link com.rainier.sprint.domain.Sprint}. */
@RestController
@RequestMapping("/api/sprints")
public class SprintController {

  private final SprintService service;

  public SprintController(SprintService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<SprintDetail> create(@Valid @RequestBody SprintCreateRequest req) {
    SprintDetail created = service.create(req);
    return ResponseEntity.created(URI.create("/api/sprints/" + created.getId())).body(created);
  }

  @GetMapping("/{id}")
  public SprintDetail get(@PathVariable Long id) {
    return service.findById(id);
  }

  @GetMapping
  public PageResponse<SprintDetail> list(
      @RequestParam(required = false) Long requirementId,
      @RequestParam(required = false) String status,
      @Valid PageParams page) {
    return service.list(requirementId, status, page);
  }

  @PutMapping("/{id}")
  public SprintDetail update(@PathVariable Long id, @Valid @RequestBody SprintUpdateRequest req) {
    return service.update(id, req);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
