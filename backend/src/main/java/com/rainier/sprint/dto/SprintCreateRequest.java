/* (C) 2026 Rainier — internal use only. */
package com.rainier.sprint.dto;

import java.time.LocalDate;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/** Payload for {@code POST /api/sprints}. */
public class SprintCreateRequest {

  @NotBlank
  @Size(max = 64)
  private String code;

  @NotBlank
  @Size(max = 100)
  private String name;

  @Size(max = 2000)
  private String description;

  @Size(max = 2000)
  private String goal;

  @Size(max = 16)
  private String status;

  @NotNull private Long requirementId;

  /** Optional — pre-binds the sprint's product at creation; otherwise established on first link. */
  private Long productId;

  @NotNull private Long ownerUserId;

  private LocalDate startDate;
  private LocalDate endDate;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getGoal() {
    return goal;
  }

  public void setGoal(String goal) {
    this.goal = goal;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Long getRequirementId() {
    return requirementId;
  }

  public void setRequirementId(Long requirementId) {
    this.requirementId = requirementId;
  }

  public Long getProductId() {
    return productId;
  }

  public void setProductId(Long productId) {
    this.productId = productId;
  }

  public Long getOwnerUserId() {
    return ownerUserId;
  }

  public void setOwnerUserId(Long ownerUserId) {
    this.ownerUserId = ownerUserId;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public void setEndDate(LocalDate endDate) {
    this.endDate = endDate;
  }
}
