/* (C) 2026 Rainier — internal use only. */
package com.rainier.event.controller;

import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.event.domain.Event;
import com.rainier.event.dto.EventCreateRequest;
import com.rainier.event.dto.EventDetail;
import com.rainier.event.repository.EventRepository;
import com.rainier.event.service.EventService;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.criteria.Predicate;
import javax.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * External event webhook + queries (v0.0.65, flywheel pipeline base). all-users (token-optional).
 * Real source-specific adapters (A2) and status-sync side-effects (A3) come later.
 */
@RestController
@RequestMapping("/api/events")
public class EventController {

  private final EventService service;
  private final EventRepository repo;

  public EventController(EventService service, EventRepository repo) {
    this.service = service;
    this.repo = repo;
  }

  @PostMapping
  public ResponseEntity<EventDetail> create(@Valid @RequestBody EventCreateRequest req) {
    Event saved =
        service.record(
            req.getSourceType(),
            req.getSourceId(),
            req.getEventKind(),
            req.getPayload(),
            req.getOccurredAt());
    return ResponseEntity.created(URI.create("/api/events/" + saved.getId()))
        .body(EventDetail.from(saved));
  }

  @GetMapping
  public PageResponse<EventDetail> list(
      @RequestParam(required = false) final String sourceType, @Valid PageParams page) {
    Specification<Event> spec =
        new Specification<Event>() {
          @Override
          public Predicate toPredicate(
              javax.persistence.criteria.Root<Event> root,
              javax.persistence.criteria.CriteriaQuery<?> q,
              javax.persistence.criteria.CriteriaBuilder cb) {
            Predicate p = cb.conjunction();
            if (sourceType != null && !sourceType.isEmpty()) {
              p = cb.and(p, cb.equal(root.get("sourceType"), sourceType));
            }
            return p;
          }
        };
    PageRequest pr =
        PageRequest.of(
            page.getPage(),
            page.getSize(),
            Sort.by(Sort.Direction.DESC, "occurredAt").and(Sort.by(Sort.Direction.DESC, "id")));
    Page<Event> result = repo.findAll(spec, pr);
    List<EventDetail> content =
        result.stream().map(EventDetail::from).collect(Collectors.toList());
    return PageResponse.of(content, page.getPage(), page.getSize(), result.getTotalElements());
  }

  @GetMapping("/pending")
  public List<EventDetail> pending(@RequestParam(defaultValue = "50") int limit) {
    return service.pending(limit).stream()
        .map(EventDetail::from)
        .collect(Collectors.toList());
  }
}
