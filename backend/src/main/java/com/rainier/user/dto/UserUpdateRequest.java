/* (C) 2026 Rainier — internal use only. */
package com.rainier.user.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** Payload for {@code PUT /api/users/{id}}. login_name is immutable post-create. */
public class UserUpdateRequest {

  @NotBlank
  @Size(max = 30)
  private String name;

  @Size(max = 30)
  private String code;

  @Email
  @Size(max = 255)
  private String emailAddress;

  private Boolean isInternal;
  private Boolean enabled;
  /**
   * v0.0.7: nullable — {@code null} clears the position; non-null requires existing Position.
   *
   * <p>NOTE: Jackson treats absent JSON keys and explicit {@code "positionId": null} the same way
   * here — both result in {@code positionId == null}. The service therefore cannot distinguish
   * "leave unchanged" from "explicitly clear"; behavior is "set to value or null based on body".
   */
  private Long positionId;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getEmailAddress() {
    return emailAddress;
  }

  public void setEmailAddress(String emailAddress) {
    this.emailAddress = emailAddress;
  }

  public Boolean getIsInternal() {
    return isInternal;
  }

  public void setIsInternal(Boolean isInternal) {
    this.isInternal = isInternal;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public Long getPositionId() {
    return positionId;
  }

  public void setPositionId(Long positionId) {
    this.positionId = positionId;
  }
}
