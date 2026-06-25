/* (C) 2026 Rainier — internal use only. */
package com.rainier.metrics.controller;

import com.rainier.metrics.dto.CrmMetrics;
import com.rainier.metrics.service.MetricsService;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * CRM / delivery health metrics (v0.0.93, D5). all-users (token-optional).
 *
 * <p>{@code periodStart}/{@code periodEnd} are ISO-8601 Instants; defaults: last 90 days through
 * now. {@code ownerUserId} filters Opportunity.commercialOwnerUserId and Project.ownerUserId.
 * {@code scope} accepted for forward-compat but currently ignored.
 */
@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

  private final MetricsService service;

  public MetricsController(MetricsService service) {
    this.service = service;
  }

  @GetMapping("/crm")
  public CrmMetrics crm(
      @RequestParam(required = false) String periodStart,
      @RequestParam(required = false) String periodEnd,
      @RequestParam(required = false) Long ownerUserId,
      @RequestParam(required = false) String scope) {
    Instant start = periodStart != null && !periodStart.isEmpty() ? Instant.parse(periodStart) : null;
    Instant end = periodEnd != null && !periodEnd.isEmpty() ? Instant.parse(periodEnd) : null;
    return service.crmSnapshot(start, end, ownerUserId);
  }
}
