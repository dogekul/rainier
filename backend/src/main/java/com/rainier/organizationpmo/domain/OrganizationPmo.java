/* (C) 2026 Rainier — internal use only. */
package com.rainier.organizationpmo.domain;

import com.rainier.common.persistence.BaseEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * v0.0.64 — 组织 PMO 关系（Organization → PMO 多对多）。一个组织可有多个 PMO；一个用户可在多个组织任 PMO。
 * 软删除；UNIQUE 由 service 层校验（组合 organization_id + user_id + del_flag=0 唯一）。
 *
 * <p>读时计算：子组织通过 OrganizationService.getAncestorIds 沿 parent_id 链向上回溯，UNION 所有祖先 PMOs
 * = effective-PMOs。删 inherited PMO 在子 org 上 SHALL 拒绝（请到上级操作）。
 */
@Entity
@Table(name = "rainier_organization_pmo")
@SQLDelete(
    sql =
        "UPDATE rainier_organization_pmo SET del_flag = 1, update_time = CURRENT_TIMESTAMP(6) WHERE id = ?")
@Where(clause = "del_flag = 0")
public class OrganizationPmo extends BaseEntity {

  @Column(name = "organization_id", nullable = false)
  private Long organizationId;

  @Column(name = "user_id", nullable = false)
  private Long userId;

  public Long getOrganizationId() {
    return organizationId;
  }

  public void setOrganizationId(Long organizationId) {
    this.organizationId = organizationId;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }
}
