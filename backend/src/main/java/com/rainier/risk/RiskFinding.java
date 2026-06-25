/* (C) 2026 Rainier — internal use only. */
package com.rainier.risk;

/**
 * Single risk hit produced by a {@link RiskRule}. Immutable POJO — pure data, no persistence. {@code
 * level} is one of {@code INFO} / {@code WARN} / {@code CRIT} (String, not enum, to match existing
 * status/linkType conventions).
 */
public class RiskFinding {

  public static final String LEVEL_INFO = "INFO";
  public static final String LEVEL_WARN = "WARN";
  public static final String LEVEL_CRIT = "CRIT";

  private final String level;
  private final String message;
  private final String entityType;
  private final Long entityId;
  private final String ruleName;

  public RiskFinding(
      String level, String message, String entityType, Long entityId, String ruleName) {
    this.level = level;
    this.message = message;
    this.entityType = entityType;
    this.entityId = entityId;
    this.ruleName = ruleName;
  }

  public String getLevel() {
    return level;
  }

  public String getMessage() {
    return message;
  }

  public String getEntityType() {
    return entityType;
  }

  public Long getEntityId() {
    return entityId;
  }

  public String getRuleName() {
    return ruleName;
  }
}
