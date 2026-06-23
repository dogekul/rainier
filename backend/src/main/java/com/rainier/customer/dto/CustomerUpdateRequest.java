/* (C) 2026 Rainier — internal use only. */
package com.rainier.customer.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** Payload for {@code PUT /api/customers/{id}} (v0.0.45). */
public class CustomerUpdateRequest {

  @NotBlank
  @Size(max = 100)
  private String name;

  @Size(max = 64)
  private String industry;

  @Size(max = 64)
  private String contactName;

  @Size(max = 1000)
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
