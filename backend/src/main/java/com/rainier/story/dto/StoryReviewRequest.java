/* (C) 2026 Rainier — internal use only. */
package com.rainier.story.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * Payload for {@code POST /api/stories/{id}/review} (v0.0.39). The one-click review decision —
 * {@code decision} must be APPROVED or REJECTED (validated in the service against {@code
 * ReviewStatus.DECISIONS}).
 */
public class StoryReviewRequest {

  @NotBlank
  @Size(max = 16)
  private String decision;

  public String getDecision() {
    return decision;
  }

  public void setDecision(String decision) {
    this.decision = decision;
  }
}
