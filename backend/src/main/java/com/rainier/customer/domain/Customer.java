/* (C) 2026 Rainier — internal use only. */
package com.rainier.customer.domain;

import com.rainier.common.persistence.BaseEntity;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * 客户 (v0.0.45 fold-in) — a customer the company sells to. Referenced (optionally) by an Opportunity
 * via {@code customerId}; the opportunity also denormalizes {@code customerName} for display, so deleting
 * a customer never breaks an existing opportunity's label. Soft-deleted.
 */
@Entity
@Table(name = "rainier_customer")
@SQLDelete(
    sql = "UPDATE rainier_customer SET del_flag = 1, update_time = CURRENT_TIMESTAMP(6) WHERE id = ?")
@Where(clause = "del_flag = 0")
public class Customer extends BaseEntity {

  @Column(nullable = false, length = 100)
  private String name;

  @Column(length = 64)
  private String industry;

  @Column(name = "contact_name", length = 64)
  private String contactName;

  @Column(length = 1000)
  private String notes;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getIndustry() {
    return industry;
  }

  public void setIndustry(String industry) {
    this.industry = industry;
  }

  public String getContactName() {
    return contactName;
  }

  public void setContactName(String contactName) {
    this.contactName = contactName;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }
}
