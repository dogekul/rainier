/* (C) 2026 Rainier — internal use only. */
package com.rainier.requirementfeature.domain;

import com.rainier.common.persistence.BaseEntity;
import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * M2M link between a {@link com.rainier.requirement.domain.Requirement} and a {@link
 * com.rainier.feature.domain.Feature} (v0.0.86, C6).
 *
 * <p>Hard delete. {@code del_flag} from {@link BaseEntity} is unused but kept for schema
 * consistency.
 *
 * <p>Unique constraint on {@code (requirement_id, feature_id)} prevents duplicate links.
 */
@Entity
@Table(
    name = "rainier_requirement_feature",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_requirement_feature",
            columnNames = {"requirement_id", "feature_id"}))
public class RequirementFeatureLink extends BaseEntity {

  @Column(name = "requirement_id", nullable = false)
  private Long requirementId;

  @Column(name = "feature_id", nullable = false)
  private Long featureId;

  /** Snapshot of link creation time (nullable; populated on insert). */
  @Column(name = "linked_at")
  private Instant linkedAt;

  /** Optional — login user id who created the link. */
  @Column(name = "linked_by_user_id")
  private Long linkedByUserId;

  public Long getRequirementId() {
    return requirementId;
  }

  public void setRequirementId(Long requirementId) {
    this.requirementId = requirementId;
  }

  public Long getFeatureId() {
    return featureId;
  }

  public void setFeatureId(Long featureId) {
    this.featureId = featureId;
  }

  public Instant getLinkedAt() {
    return linkedAt;
  }

  public void setLinkedAt(Instant linkedAt) {
    this.linkedAt = linkedAt;
  }

  public Long getLinkedByUserId() {
    return linkedByUserId;
  }

  public void setLinkedByUserId(Long linkedByUserId) {
    this.linkedByUserId = linkedByUserId;
  }
}
