/* (C) 2026 Rainier — internal use only. */
package com.rainier.email;

/** EmailSender.send 的返回值 value object（v0.0.92, D4）。 */
public class SendResult {

  private final boolean success;
  private final String providerId;
  private final String errorMessage;

  private SendResult(boolean success, String providerId, String errorMessage) {
    this.success = success;
    this.providerId = providerId;
    this.errorMessage = errorMessage;
  }

  public static SendResult success(String providerId) {
    return new SendResult(true, providerId, null);
  }

  public static SendResult failure(String errorMessage) {
    return new SendResult(false, null, errorMessage);
  }

  public boolean isSuccess() {
    return success;
  }

  public String getProviderId() {
    return providerId;
  }

  public String getErrorMessage() {
    return errorMessage;
  }
}
