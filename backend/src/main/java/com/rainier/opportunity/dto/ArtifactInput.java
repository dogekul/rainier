/* (C) 2026 Rainier — internal use only. */
package com.rainier.opportunity.dto;

import javax.validation.constraints.Size;

/**
 * Inline artifact payload carried by {@link OpportunityAdvanceRequest} (v0.0.45). Required only when the
 * source stage has a {@code TransitionArtifactRules} entry — enforced in the service (missing/blank → 400).
 */
public class ArtifactInput {

  @Size(max = 200)
  private String title;

  /** Bounded to keep the synchronous .docx export memory-safe (a 报告/纪要 is text, not a file). */
  @Size(max = 50000)
  private String content;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }
}
