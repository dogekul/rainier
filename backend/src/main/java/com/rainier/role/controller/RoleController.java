/* (C) 2026 Rainier — internal use only. */
package com.rainier.role.controller;

import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.role.dto.RoleCreateRequest;
import com.rainier.role.dto.RoleDetail;
import com.rainier.role.dto.RoleUpdateRequest;
import com.rainier.role.service.RoleService;
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

/** REST endpoints for {@link com.rainier.role.domain.Role}. */
@RestController
@RequestMapping("/api/roles")
public class RoleController {

  private final RoleService service;

  public RoleController(RoleService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<RoleDetail> create(@Valid @RequestBody RoleCreateRequest req) {
    RoleDetail created = service.create(req);
    return ResponseEntity.created(URI.create("/api/roles/" + created.getId())).body(created);
  }

  @GetMapping("/{id}")
  public RoleDetail get(@PathVariable Long id) {
    return service.findById(id);
  }

  @GetMapping
  public PageResponse<RoleDetail> list(
      @RequestParam(required = false) Boolean enabled, @Valid PageParams page) {
    return service.list(enabled, page);
  }

  @PutMapping("/{id}")
  public RoleDetail update(@PathVariable Long id, @Valid @RequestBody RoleUpdateRequest req) {
    return service.update(id, req);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
