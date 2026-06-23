/* (C) 2026 Rainier — internal use only. */
package com.rainier.opportunity.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * Payload for independently adding a 流转产出物 (v0.0.45): {@code POST /api/opportunities/{id}/artifacts}.
 * Used to prepare POC deliverables (讲解材料/甲方诉求清单/得分表/差距分析) before advancing. {@code type} must
 * be a known {@link com.rainier.opportunity.domain.ArtifactType}; LINK-kind types use {@code link}, others
 * use {@code content} — the service requires at least one non-blank.
 */
public class OpportunityArtifactCreateRequest {

  @NotBlank
  @Size(max = 32)
  private String type;

  /** Optional — 链接类材料无需标题；为空时服务端用类型名兜底。 */
  @Size(max = 200)
  private String title;

  @Size(max = 50000)
  private String content;

  @Size(max = 1000)
  private String link;

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

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

  public String getLink() {
    return link;
  }

  public void setLink(String link) {
    this.link = link;
  }
}
