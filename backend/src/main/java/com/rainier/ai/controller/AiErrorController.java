/* (C) 2026 Rainier — internal use only. */
package com.rainier.ai.controller;

import com.rainier.ai.dto.AiErrorDetail;
import com.rainier.ai.dto.AiErrorFixRequest;
import com.rainier.ai.service.AiErrorService;
import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 错误公示板 (v0.0.68, A4). 列表 GET 所有用户可见（飞轮信任契约的反面证据必须公开化）；
 * 修复 POST /&#123;id&#125;/fix 仅 admin（{@code AdminPaths} TIER_B）。
 */
@RestController
@RequestMapping("/api/ai/errors")
public class AiErrorController {

  private final AiErrorService service;

  public AiErrorController(AiErrorService service) {
    this.service = service;
  }

  @GetMapping
  public PageResponse<AiErrorDetail> list(
      @RequestParam(required = false) String status, @Valid PageParams page) {
    return service.list(status, page);
  }

  /**
   * F5 (v0.0.104) — number of OPEN errors older than {@code hours} (default 24). all-users readable
   * (Tier B → GET is open). Drives the global AppLayout overdue banner.
   */
  @GetMapping("/overdue-count")
  public java.util.Map<String, Object> overdueCount(
      @RequestParam(name = "hours", required = false, defaultValue = "24") int hours) {
    long count = service.countOverdueOpen(hours);
    java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
    body.put("count", count);
    body.put("thresholdHours", hours);
    return body;
  }

  @GetMapping("/{id}")
  public AiErrorDetail get(@PathVariable Long id) {
    return service.findById(id);
  }

  @PostMapping("/{id}/fix")
  public AiErrorDetail fix(@PathVariable Long id, @Valid @RequestBody AiErrorFixRequest req) {
    return service.markFixed(id, req.getFixAction());
  }
}
