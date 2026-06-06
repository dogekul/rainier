/* (C) 2026 Rainier — internal use only. */
package com.rainier.userrole.controller;

import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.userrole.dto.UserRoleCreateRequest;
import com.rainier.userrole.dto.UserRoleDetail;
import com.rainier.userrole.service.UserRoleService;
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

/**
 * REST endpoints for {@link com.rainier.userrole.domain.UserRole}. No PUT — M2M is delete +
 * recreate.
 */
@RestController
@RequestMapping("/api/user-roles")
public class UserRoleController {

  private final UserRoleService service;

  public UserRoleController(UserRoleService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<UserRoleDetail> create(@Valid @RequestBody UserRoleCreateRequest req) {
    UserRoleDetail created = service.create(req);
    return ResponseEntity.created(URI.create("/api/user-roles/" + created.getId())).body(created);
  }

  @GetMapping("/{id}")
  public UserRoleDetail get(@PathVariable Long id) {
    return service.findById(id);
  }

  @GetMapping
  public PageResponse<UserRoleDetail> list(
      @RequestParam(required = false) Long userId,
      @RequestParam(required = false) Long roleId,
      @RequestParam(required = false) Long projectId,
      @Valid PageParams page) {
    return service.list(userId, roleId, projectId, page);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
