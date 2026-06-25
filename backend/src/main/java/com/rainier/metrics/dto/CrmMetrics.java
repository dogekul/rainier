/* (C) 2026 Rainier — internal use only. */
package com.rainier.metrics.dto;

import java.util.List;

/**
 * Aggregate CRM/delivery health snapshot (v0.0.93, D5). All fields nullable when there are no
 * samples for the period — frontend renders "—".
 */
public class CrmMetrics {

  private Double winRate; // WON / (WON+LOST) in period; null if no closed deals
  private Double dealRate; // WON / total in period; null if empty
  private Double avgDeliveryCycleDays; // average (endDate-startDate) of DELIVERED projects in period; null if none
  private List<OverdueProjectRow> overdueProjects;

  public Double getWinRate() {
    return winRate;
  }

  public void setWinRate(Double winRate) {
    this.winRate = winRate;
  }

  public Double getDealRate() {
    return dealRate;
  }

  public void setDealRate(Double dealRate) {
    this.dealRate = dealRate;
  }

  public Double getAvgDeliveryCycleDays() {
    return avgDeliveryCycleDays;
  }

  public void setAvgDeliveryCycleDays(Double avgDeliveryCycleDays) {
    this.avgDeliveryCycleDays = avgDeliveryCycleDays;
  }

  public List<OverdueProjectRow> getOverdueProjects() {
    return overdueProjects;
  }

  public void setOverdueProjects(List<OverdueProjectRow> overdueProjects) {
    this.overdueProjects = overdueProjects;
  }
}
