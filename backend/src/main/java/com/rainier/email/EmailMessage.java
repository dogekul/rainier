/* (C) 2026 Rainier — internal use only. */
package com.rainier.email;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 邮件消息 value object（v0.0.92, D4）。简单 POJO，不带验证 —— 调用方负责合法性。
 *
 * <p>{@code to} 至少一条；{@code cc} 可空；body 提供 text 或 html 之一即可。
 */
public class EmailMessage {

  private String from;
  private List<String> to = new ArrayList<String>();
  private List<String> cc = new ArrayList<String>();
  private String subject;
  private String bodyText;
  private String bodyHtml;

  public EmailMessage() {}

  public EmailMessage(String from, List<String> to, String subject, String bodyText) {
    this.from = from;
    this.to = (to == null) ? new ArrayList<String>() : new ArrayList<String>(to);
    this.subject = subject;
    this.bodyText = bodyText;
  }

  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public List<String> getTo() {
    return to == null ? Collections.<String>emptyList() : to;
  }

  public void setTo(List<String> to) {
    this.to = to;
  }

  public List<String> getCc() {
    return cc == null ? Collections.<String>emptyList() : cc;
  }

  public void setCc(List<String> cc) {
    this.cc = cc;
  }

  public String getSubject() {
    return subject;
  }

  public void setSubject(String subject) {
    this.subject = subject;
  }

  public String getBodyText() {
    return bodyText;
  }

  public void setBodyText(String bodyText) {
    this.bodyText = bodyText;
  }

  public String getBodyHtml() {
    return bodyHtml;
  }

  public void setBodyHtml(String bodyHtml) {
    this.bodyHtml = bodyHtml;
  }
}
