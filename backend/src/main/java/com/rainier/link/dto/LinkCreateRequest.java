/* (C) 2026 Rainier — internal use only. */
package com.rainier.link.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/** Payload for {@code POST /api/links} (v0.0.31). */
public class LinkCreateRequest {

  @NotBlank private String targetType;

  @NotNull private Long targetId;

  @NotBlank private String linkType;

  @Size(max = 200)
  private String label;

  @NotBlank
  @Size(max = 1000)
  private String url;

  public String getTargetType() {
    return targetType;
  }

  public void setTargetType(String targetType) {
    this.targetType = targetType;
  }

  public Long getTargetId() {
    return targetId;
  }

  public void setTargetId(Long targetId) {
    this.targetId = targetId;
  }

  public String getLinkType() {
    return linkType;
  }

  public void setLinkType(String linkType) {
    this.linkType = linkType;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }
}
