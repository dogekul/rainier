/* (C) 2026 Rainier — internal use only. */
package com.rainier.sprintfeature.domain;

import com.rainier.common.persistence.BaseEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * M2M link between a {@link com.rainier.sprint.domain.Sprint} (产品迭代) and a {@link
 * com.rainier.feature.domain.Feature}, bridging the project domain and the product domain
 * (v0.0.14).
 *
 * <p>Hard delete (no {@code @SQLDelete}/{@code @Where}) — same as DemandRequirementLink. The {@code
 * del_flag} column inherited from {@link BaseEntity} is unused but kept for schema consistency.
 *
 * <p>Unique constraint on {@code (sprint_id, feature_id)} prevents duplicate links.
 */
@Entity
@Table(
    name = "rainier_sprint_feature",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_sprint_feature",
            columnNames = {"sprint_id", "feature_id"}))
public class SprintFeatureLink extends BaseEntity {

  @Column(name = "sprint_id", nullable = false)
  private Long sprintId;

  @Column(name = "feature_id", nullable = false)
  private Long featureId;

  public Long getSprintId() {
    return sprintId;
  }

  public void setSprintId(Long sprintId) {
    this.sprintId = sprintId;
  }

  public Long getFeatureId() {
    return featureId;
  }

  public void setFeatureId(Long featureId) {
    this.featureId = featureId;
  }
}
