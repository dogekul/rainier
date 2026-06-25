/* (C) 2026 Rainier — internal use only. */
package com.rainier.capability.dto;

/** v0.0.85 (C5) — read-model: one user × tag row with the tag's name/category joined in. */
public class UserCapabilityDto {
  private Long id;
  private Long userId;
  private Long capabilityTagId;
  private String tagName;
  private String tagCategory;
  private Integer level;
  private String source;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public Long getCapabilityTagId() {
    return capabilityTagId;
  }

  public void setCapabilityTagId(Long capabilityTagId) {
    this.capabilityTagId = capabilityTagId;
  }

  public String getTagName() {
    return tagName;
  }

  public void setTagName(String tagName) {
    this.tagName = tagName;
  }

  public String getTagCategory() {
    return tagCategory;
  }

  public void setTagCategory(String tagCategory) {
    this.tagCategory = tagCategory;
  }

  public Integer getLevel() {
    return level;
  }

  public void setLevel(Integer level) {
    this.level = level;
  }

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }
}
