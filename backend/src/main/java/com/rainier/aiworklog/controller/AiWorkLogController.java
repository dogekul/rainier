/* (C) 2026 Rainier — internal use only. */
package com.rainier.aiworklog.controller;

import com.rainier.aiworklog.dto.AiWorkLogCreateRequest;
import com.rainier.aiworklog.dto.AiWorkLogDecisionRequest;
import com.rainier.aiworklog.dto.AiWorkLogDetail;
import com.rainier.aiworklog.service.AiWorkLogService;
import com.rainier.auth.controller.AuthController;
import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import java.net.URI;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI work log (v0.0.43, flywheel base). all-users (token-gated, not admin): everyone can see AI
 * proposals and accept/reject them. Fine-grained 分级授权 is a later flywheel step.
 */
@RestController
@RequestMapping("/api/ai-work-logs")
public class AiWorkLogController {

  private final AiWorkLogService service;

  public AiWorkLogController(AiWorkLogService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<AiWorkLogDetail> create(@Valid @RequestBody AiWorkLogCreateRequest req) {
    AiWorkLogDetail created = service.create(req);
    return ResponseEntity.created(URI.create("/api/ai-work-logs/" + created.getId())).body(created);
  }

  @GetMapping
  public PageResponse<AiWorkLogDetail> list(
      @RequestParam(required = false) String agentType,
      @RequestParam(required = false) String status,
      @Valid PageParams page) {
    return service.query(agentType, status, page);
  }

  @GetMapping("/{id}")
  public AiWorkLogDetail get(@PathVariable Long id) {
    return service.findById(id);
  }

  @PostMapping("/{id}/decision")
  public AiWorkLogDetail decide(
      @PathVariable Long id,
      @Valid @RequestBody AiWorkLogDecisionRequest req,
      HttpServletRequest request) {
    return service.decide(id, req.getDecision(), req.getReason(), currentUser(request));
  }

  /** The decider's identity (token subject), falling back to "system" for unauthenticated contexts. */
  private static String currentUser(HttpServletRequest request) {
    Object u = request.getAttribute(AuthController.ATTR_USERNAME);
    return (u instanceof String && !((String) u).isEmpty()) ? (String) u : "system";
  }
}
