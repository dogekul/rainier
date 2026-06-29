/* (C) 2026 Rainier — internal use only. */
package com.rainier.pmo;

import com.rainier.organization.domain.Organization;
import com.rainier.organization.repository.OrganizationRepository;
import com.rainier.portfolio.PortfolioRow;
import com.rainier.portfolio.PortfolioService;
import com.rainier.portfolio.ScopeService;
import com.rainier.project.domain.Project;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * v0.0.110 (H3) — rolls the company-wide portfolio into pivoted groups for the PMO map. Reuses
 * {@link ScopeService}.scope="all" + {@link PortfolioService} for the per-project rollup and groups
 * by Project.organizationId / ownerUserId / none.
 */
@Service
@Transactional(readOnly = true)
public class PmoPortfolioService {

  /** Pivot dimensions supported by the PMO map. */
  public enum GroupBy {
    ORGANIZATION,
    OWNER,
    NONE;

    /** Parse query param leniently. Unknown values fall back to {@link #NONE}. */
    public static GroupBy parse(String raw) {
      if (raw == null) {
        return NONE;
      }
      String s = raw.trim().toLowerCase();
      if ("organization".equals(s) || "org".equals(s)) {
        return ORGANIZATION;
      }
      if ("owner".equals(s)) {
        return OWNER;
      }
      return NONE;
    }
  }

  private final ScopeService scopeService;
  private final PortfolioService portfolioService;
  private final ProjectRepository projectRepo;
  private final OrganizationRepository orgRepo;
  private final UserRepository userRepo;

  public PmoPortfolioService(
      ScopeService scopeService,
      PortfolioService portfolioService,
      ProjectRepository projectRepo,
      OrganizationRepository orgRepo,
      UserRepository userRepo) {
    this.scopeService = scopeService;
    this.portfolioService = portfolioService;
    this.projectRepo = projectRepo;
    this.orgRepo = orgRepo;
    this.userRepo = userRepo;
  }

  /**
   * Build the PMO company map. {@code username} is needed only to seed {@link ScopeService}; scope
   * is always {@code all} (PMO sees everything).
   */
  public List<PmoPortfolioRow> companyMap(String username, GroupBy groupBy) {
    List<Long> projectIds = scopeService.resolveProjectIds(username, "all");
    List<PortfolioRow> rows = portfolioService.portfolio(projectIds);
    if (groupBy == GroupBy.NONE || rows.isEmpty()) {
      PmoPortfolioRow.Group all = new PmoPortfolioRow.Group(null, "全公司", null);
      return singleGroup(all, rows);
    }
    if (groupBy == GroupBy.ORGANIZATION) {
      return groupByOrganization(rows);
    }
    return groupByOwner(rows);
  }

  private List<PmoPortfolioRow> groupByOrganization(List<PortfolioRow> rows) {
    Map<Long, List<PortfolioRow>> byOrg = new LinkedHashMap<>();
    for (PortfolioRow r : rows) {
      Long key = r.getOrganizationId();
      List<PortfolioRow> bucket = byOrg.get(key);
      if (bucket == null) {
        bucket = new ArrayList<>();
        byOrg.put(key, bucket);
      }
      bucket.add(r);
    }
    Map<Long, Organization> orgIndex = new HashMap<>();
    List<Long> orgIds = new ArrayList<>();
    for (Long id : byOrg.keySet()) {
      if (id != null) {
        orgIds.add(id);
      }
    }
    for (Organization o : orgRepo.findAllById(orgIds)) {
      orgIndex.put(o.getId(), o);
    }
    List<PmoPortfolioRow> out = new ArrayList<>();
    for (Map.Entry<Long, List<PortfolioRow>> e : byOrg.entrySet()) {
      Long id = e.getKey();
      PmoPortfolioRow.Group g;
      if (id == null) {
        g = new PmoPortfolioRow.Group(null, "未归属", null);
      } else {
        Organization org = orgIndex.get(id);
        String name = org != null ? org.getName() : ("Org#" + id);
        String type = org != null && org.getType() != null ? org.getType().name() : null;
        g = new PmoPortfolioRow.Group(id, name, type);
      }
      out.add(new PmoPortfolioRow(g, e.getValue(), tally(e.getValue())));
    }
    sortGroupsByWorst(out);
    return out;
  }

  private List<PmoPortfolioRow> groupByOwner(List<PortfolioRow> rows) {
    // ownerUserId lives on Project, not PortfolioRow — fetch the projects we need.
    Map<Long, Long> projectIdToOwner = new HashMap<>();
    List<Long> projectIds = new ArrayList<>();
    for (PortfolioRow r : rows) {
      projectIds.add(r.getProjectId());
    }
    for (Project p : projectRepo.findAllById(projectIds)) {
      projectIdToOwner.put(p.getId(), p.getOwnerUserId());
    }
    Map<Long, List<PortfolioRow>> byOwner = new LinkedHashMap<>();
    for (PortfolioRow r : rows) {
      Long owner = projectIdToOwner.get(r.getProjectId());
      List<PortfolioRow> bucket = byOwner.get(owner);
      if (bucket == null) {
        bucket = new ArrayList<>();
        byOwner.put(owner, bucket);
      }
      bucket.add(r);
    }
    Map<Long, User> userIndex = new HashMap<>();
    List<Long> userIds = new ArrayList<>();
    for (Long id : byOwner.keySet()) {
      if (id != null) {
        userIds.add(id);
      }
    }
    for (User u : userRepo.findAllById(userIds)) {
      userIndex.put(u.getId(), u);
    }
    List<PmoPortfolioRow> out = new ArrayList<>();
    for (Map.Entry<Long, List<PortfolioRow>> e : byOwner.entrySet()) {
      Long id = e.getKey();
      PmoPortfolioRow.Group g;
      if (id == null) {
        g = new PmoPortfolioRow.Group(null, "未指定", "USER");
      } else {
        User u = userIndex.get(id);
        String name = u != null ? u.getName() : ("User#" + id);
        g = new PmoPortfolioRow.Group(id, name, "USER");
      }
      out.add(new PmoPortfolioRow(g, e.getValue(), tally(e.getValue())));
    }
    sortGroupsByWorst(out);
    return out;
  }

  private static List<PmoPortfolioRow> singleGroup(
      PmoPortfolioRow.Group g, List<PortfolioRow> rows) {
    List<PmoPortfolioRow> out = new ArrayList<>();
    out.add(new PmoPortfolioRow(g, rows, tally(rows)));
    return out;
  }

  private static PmoPortfolioRow.RygCount tally(List<PortfolioRow> rows) {
    int red = 0;
    int yellow = 0;
    int green = 0;
    int gray = 0;
    for (PortfolioRow r : rows) {
      String ryg = r.getRyg();
      if ("RED".equals(ryg)) {
        red++;
      } else if ("YELLOW".equals(ryg)) {
        yellow++;
      } else if ("GREEN".equals(ryg)) {
        green++;
      } else {
        gray++;
      }
    }
    return new PmoPortfolioRow.RygCount(red, yellow, green, gray);
  }

  /** Worst-first: groups with more RED rank first; tie-break on YELLOW, then group name. */
  private static void sortGroupsByWorst(List<PmoPortfolioRow> rows) {
    rows.sort(
        Comparator.comparingInt((PmoPortfolioRow r) -> -r.getRygCount().getRed())
            .thenComparingInt(r -> -r.getRygCount().getYellow())
            .thenComparing(r -> r.getGroup().getName() == null ? "" : r.getGroup().getName()));
  }
}
