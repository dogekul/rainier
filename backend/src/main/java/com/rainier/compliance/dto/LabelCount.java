/* (C) 2026 Rainier — internal use only. */
package com.rainier.compliance.dto;

/** One bucket in an audit-aggregation breakdown (v0.0.41): a label and its row count. */
public class LabelCount {

  private final String label;
  private final long count;

  public LabelCount(String label, long count) {
    this.label = label;
    this.count = count;
  }

  public String getLabel() {
    return label;
  }

  public long getCount() {
    return count;
  }
}
