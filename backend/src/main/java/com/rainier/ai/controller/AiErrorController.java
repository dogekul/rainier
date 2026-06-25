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

  @GetMapping("/{id}")
  public AiErrorDetail get(@PathVariable Long id) {
    return service.findById(id);
  }

  @PostMapping("/{id}/fix")
  public AiErrorDetail fix(@PathVariable Long id, @Valid @RequestBody AiErrorFixRequest req) {
    return service.markFixed(id, req.getFixAction());
  }
}
