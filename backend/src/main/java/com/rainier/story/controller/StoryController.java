/* (C) 2026 Rainier — internal use only. */
package com.rainier.story.controller;

import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.story.dto.StoryCreateRequest;
import com.rainier.story.dto.StoryDetail;
import com.rainier.story.dto.StoryUpdateRequest;
import com.rainier.story.service.StoryService;
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

/** REST endpoints for {@link com.rainier.story.domain.Story}. */
@RestController
@RequestMapping("/api/stories")
public class StoryController {

  private final StoryService service;

  public StoryController(StoryService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<StoryDetail> create(@Valid @RequestBody StoryCreateRequest req) {
    StoryDetail created = service.create(req);
    return ResponseEntity.created(URI.create("/api/stories/" + created.getId())).body(created);
  }

  @GetMapping("/{id}")
  public StoryDetail get(@PathVariable Long id) {
    return service.findById(id);
  }

  @GetMapping
  public PageResponse<StoryDetail> list(
      @RequestParam(required = false) Long requirementId,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String priority,
      @Valid PageParams page) {
    return service.list(requirementId, status, priority, page);
  }

  @PutMapping("/{id}")
  public StoryDetail update(@PathVariable Long id, @Valid @RequestBody StoryUpdateRequest req) {
    return service.update(id, req);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
