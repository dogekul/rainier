/* (C) 2026 Rainier — internal use only. */
package com.rainier.task.controller;

import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.task.dto.TaskCreateRequest;
import com.rainier.task.dto.TaskDetail;
import com.rainier.task.dto.TaskUpdateRequest;
import com.rainier.task.service.TaskService;
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

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

  private final TaskService service;

  public TaskController(TaskService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<TaskDetail> create(@Valid @RequestBody TaskCreateRequest req) {
    TaskDetail created = service.create(req);
    return ResponseEntity.created(URI.create("/api/tasks/" + created.getId())).body(created);
  }

  @GetMapping("/{id}")
  public TaskDetail get(@PathVariable Long id) {
    return service.findById(id);
  }

  @GetMapping
  public PageResponse<TaskDetail> list(
      @RequestParam(required = false) Long projectId,
      @RequestParam(required = false) Long sprintId,
      @RequestParam(required = false) Long storyId,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String priority,
      @RequestParam(required = false) Long assigneeUserId,
      PageParams page) {
    return service.list(projectId, sprintId, storyId, status, priority, assigneeUserId, page);
  }

  @PutMapping("/{id}")
  public TaskDetail update(@PathVariable Long id, @Valid @RequestBody TaskUpdateRequest req) {
    return service.update(id, req);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
