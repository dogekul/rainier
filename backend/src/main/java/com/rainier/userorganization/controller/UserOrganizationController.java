/* (C) 2026 Rainier — internal use only. */
package com.rainier.userorganization.controller;

import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.userorganization.domain.UserOrgRole;
import com.rainier.userorganization.dto.UserOrgCreateRequest;
import com.rainier.userorganization.dto.UserOrgDetail;
import com.rainier.userorganization.dto.UserOrgUpdateRequest;
import com.rainier.userorganization.service.UserOrganizationService;
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

/** REST endpoints for the user ↔ organization M2M. */
@RestController
@RequestMapping("/api/user-organizations")
public class UserOrganizationController {

  private final UserOrganizationService service;

  public UserOrganizationController(UserOrganizationService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<UserOrgDetail> create(@Valid @RequestBody UserOrgCreateRequest req) {
    UserOrgDetail created = service.create(req);
    return ResponseEntity.created(URI.create("/api/user-organizations/" + created.getId()))
        .body(created);
  }

  @GetMapping("/{id}")
  public UserOrgDetail get(@PathVariable String id) {
    return service.findById(id);
  }

  @GetMapping
  public PageResponse<UserOrgDetail> list(
      @RequestParam(required = false) String userId,
      @RequestParam(required = false) String organizationId,
      @RequestParam(required = false) UserOrgRole role,
      @RequestParam(required = false) Boolean activeOnly,
      @Valid PageParams page) {
    return service.list(userId, organizationId, role, activeOnly, page);
  }

  @PutMapping("/{id}")
  public UserOrgDetail update(
      @PathVariable String id, @Valid @RequestBody UserOrgUpdateRequest req) {
    return service.update(id, req);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable String id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
