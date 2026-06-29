/* (C) 2026 Rainier — internal use only. */
package com.rainier.pmo;

import com.rainier.portfolio.PortfolioRow;
import java.util.List;

/**
 * v0.0.110 (H3) — one group on the PMO company project map. The group descriptor identifies the
 * pivot dimension (organization / owner / none) along with the rolled RYG counts and the underlying
 * {@link PortfolioRow}s. Reused by {@code GET /api/pmo/portfolio?groupBy=...}.
 */
public class PmoPortfolioRow {

  private final Group group;
  private final List<PortfolioRow> projects;
  private final RygCount rygCount;

  public PmoPortfolioRow(Group group, List<PortfolioRow> projects, RygCount rygCount) {
    this.group = group;
    this.projects = projects;
    this.rygCount = rygCount;
  }

  public Group getGroup() {
    return group;
  }

  public List<PortfolioRow> getProjects() {
    return projects;
  }

  public RygCount getRygCount() {
    return rygCount;
  }

  /** Group descriptor (organization, owner, or the synthetic "全公司" pseudo-group). */
  public static class Group {
    private final Long id;
    private final String name;
    private final String type;

    public Group(Long id, String name, String type) {
      this.id = id;
      this.name = name;
      this.type = type;
    }

    public Long getId() {
      return id;
    }

    public String getName() {
      return name;
    }

    public String getType() {
      return type;
    }
  }

  /** RYG tally over a group's projects. */
  public static class RygCount {
    private final int red;
    private final int yellow;
    private final int green;
    private final int gray;

    public RygCount(int red, int yellow, int green, int gray) {
      this.red = red;
      this.yellow = yellow;
      this.green = green;
      this.gray = gray;
    }

    public int getRed() {
      return red;
    }

    public int getYellow() {
      return yellow;
    }

    public int getGreen() {
      return green;
    }

    public int getGray() {
      return gray;
    }
  }
}
