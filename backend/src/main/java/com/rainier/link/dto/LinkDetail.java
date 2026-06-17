/* (C) 2026 Rainier — internal use only. */
package com.rainier.link.dto;

import com.rainier.link.domain.EntityLink;
import java.time.Instant;

/** Response DTO for {@link EntityLink} (v0.0.31). */
public class LinkDetail {

  private Long id;
  private String targetType;
  private Long targetId;
  private String linkType;
  private String label;
  private String url;
  private Instant createTime;
  private String createBy;

  public static LinkDetail from(EntityLink l) {
    LinkDetail dto = new LinkDetail();
    dto.id = l.getId();
    dto.targetType = l.getTargetType();
    dto.targetId = l.getTargetId();
    dto.linkType = l.getLinkType();
    dto.label = l.getLabel();
    dto.url = l.getUrl();
    dto.createTime = l.getCreateTime();
    dto.createBy = l.getCreateBy();
    return dto;
  }

  public Long getId() {
    return id;
  }

  public String getTargetType() {
    return targetType;
  }

  public Long getTargetId() {
    return targetId;
  }

  public String getLinkType() {
    return linkType;
  }

  public String getLabel() {
    return label;
  }

  public String getUrl() {
    return url;
  }

  public Instant getCreateTime() {
    return createTime;
  }

  public String getCreateBy() {
    return createBy;
  }
}
