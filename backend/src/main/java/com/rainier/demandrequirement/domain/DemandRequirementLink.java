/* (C) 2026 Rainier — internal use only. */
package com.rainier.demandrequirement.domain;

import com.rainier.common.persistence.BaseEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * M2M link between a {@link com.rainier.demand.domain.Demand} and a {@link
 * com.rainier.requirement.domain.Requirement}.
 *
 * <p>Hard delete (no {@code @SQLDelete}/{@code @Where}) — see design.md decision 1. The {@code
 * del_flag} column inherited from {@link BaseEntity} is unused but kept for schema consistency.
 *
 * <p>Unique constraint on {@code (demand_id, requirement_id)} prevents duplicate links.
 */
@Entity
@Table(
    name = "rainier_demand_requirement",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_demand_requirement",
            columnNames = {"demand_id", "requirement_id"}))
public class DemandRequirementLink extends BaseEntity {

  @Column(name = "demand_id", nullable = false)
  private Long demandId;

  @Column(name = "requirement_id", nullable = false)
  private Long requirementId;

  @Column(name = "link_type", nullable = false, length = 16)
  private String linkType;

  public Long getDemandId() {
    return demandId;
  }

  public void setDemandId(Long demandId) {
    this.demandId = demandId;
  }

  public Long getRequirementId() {
    return requirementId;
  }

  public void setRequirementId(Long requirementId) {
    this.requirementId = requirementId;
  }

  public String getLinkType() {
    return linkType;
  }

  public void setLinkType(String linkType) {
    this.linkType = linkType;
  }
}
