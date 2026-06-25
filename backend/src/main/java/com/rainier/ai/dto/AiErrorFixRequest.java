/* (C) 2026 Rainier — internal use only. */
package com.rainier.ai.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** Payload for {@code POST /api/ai/errors/&#123;id&#125;/fix} (v0.0.68, A4). */
public class AiErrorFixRequest {

  @NotBlank
  @Size(max = 512)
  private String fixAction;

  public String getFixAction() {
    return fixAction;
  }

  public void setFixAction(String fixAction) {
    this.fixAction = fixAction;
  }
}
