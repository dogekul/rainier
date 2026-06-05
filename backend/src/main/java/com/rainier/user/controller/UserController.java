/* (C) 2026 Rainier — internal use only. */
package com.rainier.user.controller;

import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.user.dto.UserCreateRequest;
import com.rainier.user.dto.UserDetail;
import com.rainier.user.dto.UserUpdateRequest;
import com.rainier.user.service.UserService;
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

/** REST endpoints for {@link com.rainier.user.domain.User}. */
@RestController
@RequestMapping("/api/users")
public class UserController {

  private final UserService service;

  public UserController(UserService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<UserDetail> create(@Valid @RequestBody UserCreateRequest req) {
    UserDetail created = service.create(req);
    return ResponseEntity.created(URI.create("/api/users/" + created.getId())).body(created);
  }

  @GetMapping("/{id}")
  public UserDetail get(@PathVariable String id) {
    return service.findById(id);
  }

  @GetMapping
  public PageResponse<UserDetail> list(
      @RequestParam(required = false) Boolean isInternal,
      @RequestParam(required = false) Boolean enabled,
      @Valid PageParams page) {
    return service.list(isInternal, enabled, page);
  }

  @PutMapping("/{id}")
  public UserDetail update(@PathVariable String id, @Valid @RequestBody UserUpdateRequest req) {
    return service.update(id, req);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable String id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
