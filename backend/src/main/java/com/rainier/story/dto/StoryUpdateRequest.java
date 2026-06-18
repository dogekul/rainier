/* (C) 2026 Rainier — internal use only. */
package com.rainier.story.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Payload for {@code PUT /api/stories/{id}}. {@code requirementId} and {@code projectId} are NOT
 * accepted — they are immutable after creation. {@code ownerUserId} IS mutable (sibling of v0.0.8
 * Decision 6b); Service validates the new user exists.
 */
public class StoryUpdateRequest {

  @NotBlank
  @Size(max = 64)
  private String code;

  @NotBlank
  @Size(max = 100)
  private String title;

  @Size(max = 4000)
  private String description;

  @Size(max = 4000)
  private String acceptanceCriteria;

  @NotBlank
  @Size(max = 16)
  private String status;

  @NotBlank
  @Size(max = 16)
  private String priority;

  @Size(max = 8)
  private String complexity;

  @NotNull private Long ownerUserId;

  /** v0.0.39: optional reviewer (full-replace; validated to exist when non-null). */
  private Long reviewerUserId;

  /** v0.0.39: optional review state (full-replace; validated against ReviewStatus.ALL when non-null). */
  @Size(max = 16)
  private String reviewStatus;

  @Size(max = 500)
  private String closeReason;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getAcceptanceCriteria() {
    return acceptanceCriteria;
  }

  public void setAcceptanceCriteria(String acceptanceCriteria) {
    this.acceptanceCriteria = acceptanceCriteria;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getPriority() {
    return priority;
  }

  public void setPriority(String priority) {
    this.priority = priority;
  }

  public String getComplexity() {
    return complexity;
  }

  public void setComplexity(String complexity) {
    this.complexity = complexity;
  }

  public Long getOwnerUserId() {
    return ownerUserId;
  }

  public void setOwnerUserId(Long ownerUserId) {
    this.ownerUserId = ownerUserId;
  }

  public Long getReviewerUserId() {
    return reviewerUserId;
  }

  public void setReviewerUserId(Long reviewerUserId) {
    this.reviewerUserId = reviewerUserId;
  }

  public String getReviewStatus() {
    return reviewStatus;
  }

  public void setReviewStatus(String reviewStatus) {
    this.reviewStatus = reviewStatus;
  }

  public String getCloseReason() {
    return closeReason;
  }

  public void setCloseReason(String closeReason) {
    this.closeReason = closeReason;
  }
}
