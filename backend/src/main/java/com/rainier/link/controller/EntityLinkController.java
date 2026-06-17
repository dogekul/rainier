/* (C) 2026 Rainier — internal use only. */
package com.rainier.link.controller;

import com.rainier.link.dto.LinkCreateRequest;
import com.rainier.link.dto.LinkDetail;
import com.rainier.link.service.EntityLinkService;
import java.net.URI;
import java.util.List;
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
 * v0.0.31 关联面板 — minimal external-artifact links on a Story/Task. All-users (everyone relates their
 * own artifacts); no update endpoint (a link is delete-and-readd).
 */
@RestController
@RequestMapping("/api/links")
public class EntityLinkController {

  private final EntityLinkService service;

  public EntityLinkController(EntityLinkService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<LinkDetail> create(@Valid @RequestBody LinkCreateRequest req) {
    LinkDetail created = service.create(req);
    return ResponseEntity.created(URI.create("/api/links/" + created.getId())).body(created);
  }

  @GetMapping
  public List<LinkDetail> list(
      @RequestParam String targetType, @RequestParam Long targetId) {
    return service.list(targetType, targetId);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
