/* (C) 2026 Rainier — internal use only. */
package com.rainier.customer.dto;

import com.rainier.customer.domain.Customer;
import java.time.Instant;

/** Read view of a {@link Customer} (v0.0.45). */
public class CustomerDetail {

  private Long id;
  private String name;
  private String industry;
  private String contactName;
  private String notes;
  private Instant createTime;
  private Instant updateTime;

  public static CustomerDetail from(Customer c) {
    CustomerDetail d = new CustomerDetail();
    d.id = c.getId();
    d.name = c.getName();
    d.industry = c.getIndustry();
    d.contactName = c.getContactName();
    d.notes = c.getNotes();
    d.createTime = c.getCreateTime();
    d.updateTime = c.getUpdateTime();
    return d;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getIndustry() {
    return industry;
  }

  public String getContactName() {
    return contactName;
  }

  public String getNotes() {
    return notes;
  }

  public Instant getCreateTime() {
    return createTime;
  }

  public Instant getUpdateTime() {
    return updateTime;
  }
}
