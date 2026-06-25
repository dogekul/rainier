/* (C) 2026 Rainier — internal use only. */
package com.rainier.metrics.service;

import com.rainier.metrics.dto.CrmMetrics;
import com.rainier.metrics.dto.OverdueProjectRow;
import com.rainier.opportunity.domain.Opportunity;
import com.rainier.opportunity.domain.OpportunityStatus;
import com.rainier.opportunity.repository.OpportunityRepository;
import com.rainier.project.domain.Project;
import com.rainier.project.domain.ProjectStatus;
import com.rainier.project.repository.ProjectRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aggregate CRM/delivery health metrics (v0.0.93, D5).
 *
 * <ul>
 *   <li>winRate / dealRate over an Opportunity.createTime period
 *   <li>avgDeliveryCycleDays over Projects whose endDate falls in the period and status=DELIVERED
 *   <li>overdueProjects = Project.endDate &lt; today AND status NOT IN (DELIVERED, ARCHIVED)
 * </ul>
 *
 * <p>{@code ownerUserId} filter matches Opportunity.commercialOwnerUserId AND Project.ownerUserId.
 */
@Service
@Transactional(readOnly = true)
public class MetricsService {

  private static final Set<String> CLOSED_PROJECT_STATUSES =
      new HashSet<String>(Arrays.asList(ProjectStatus.DELIVERED, ProjectStatus.ARCHIVED));

  private final OpportunityRepository oppRepo;
  private final ProjectRepository projectRepo;

  public MetricsService(OpportunityRepository oppRepo, ProjectRepository projectRepo) {
    this.oppRepo = oppRepo;
    this.projectRepo = projectRepo;
  }

  /** Middle of the dashboard: full CRM snapshot. */
  public CrmMetrics crmSnapshot(Instant periodStart, Instant periodEnd, Long ownerUserId) {
    Instant end = periodEnd != null ? periodEnd : Instant.now();
    Instant start = periodStart != null ? periodStart : end.minus(90, ChronoUnit.DAYS);

    CrmMetrics m = new CrmMetrics();
    m.setWinRate(winRate(start, end, ownerUserId));
    m.setDealRate(dealRate(start, end, ownerUserId));
    m.setAvgDeliveryCycleDays(avgDeliveryCycleDays(start, end, ownerUserId));
    m.setOverdueProjects(overdueProjects(ownerUserId));
    return m;
  }

  /** WON / (WON+LOST) over period; null when no closed deals. */
  public Double winRate(Instant periodStart, Instant periodEnd, Long ownerUserId) {
    long won = countOpportunities(periodStart, periodEnd, ownerUserId, OpportunityStatus.WON);
    long lost = countOpportunities(periodStart, periodEnd, ownerUserId, OpportunityStatus.LOST);
    long denom = won + lost;
    if (denom == 0L) {
      return null;
    }
    return ((double) won) / ((double) denom);
  }

  /** WON / total over period; null when no opportunities. */
  public Double dealRate(Instant periodStart, Instant periodEnd, Long ownerUserId) {
    long won = countOpportunities(periodStart, periodEnd, ownerUserId, OpportunityStatus.WON);
    long total = countOpportunities(periodStart, periodEnd, ownerUserId, null);
    if (total == 0L) {
      return null;
    }
    return ((double) won) / ((double) total);
  }

  /**
   * Average (endDate-startDate) in days across DELIVERED projects whose endDate lies in [start,
   * end). Null when no samples. Skips rows missing startDate or endDate.
   */
  public Double avgDeliveryCycleDays(Instant periodStart, Instant periodEnd, Long ownerUserId) {
    LocalDate startDay = periodStart.atZone(ZoneId.systemDefault()).toLocalDate();
    LocalDate endDay = periodEnd.atZone(ZoneId.systemDefault()).toLocalDate();
    Specification<Project> spec =
        new Specification<Project>() {
          @Override
          public Predicate toPredicate(
              javax.persistence.criteria.Root<Project> root,
              javax.persistence.criteria.CriteriaQuery<?> q,
              javax.persistence.criteria.CriteriaBuilder cb) {
            Predicate p = cb.equal(root.get("status"), ProjectStatus.DELIVERED);
            p = cb.and(p, cb.isNotNull(root.get("startDate")));
            p = cb.and(p, cb.isNotNull(root.get("endDate")));
            p = cb.and(p, cb.greaterThanOrEqualTo(root.<LocalDate>get("endDate"), startDay));
            p = cb.and(p, cb.lessThan(root.<LocalDate>get("endDate"), endDay));
            if (ownerUserId != null) {
              p = cb.and(p, cb.equal(root.get("ownerUserId"), ownerUserId));
            }
            return p;
          }
        };
    List<Project> rows = projectRepo.findAll(spec);
    if (rows.isEmpty()) {
      return null;
    }
    long totalDays = 0L;
    long n = 0L;
    for (Project p : rows) {
      long d = ChronoUnit.DAYS.between(p.getStartDate(), p.getEndDate());
      totalDays += d;
      n++;
    }
    if (n == 0L) {
      return null;
    }
    return ((double) totalDays) / ((double) n);
  }

  /** Projects whose expected end date is past and that aren't DELIVERED/ARCHIVED. */
  public List<OverdueProjectRow> overdueProjects(Long ownerUserId) {
    LocalDate today = LocalDate.now();
    Specification<Project> spec =
        new Specification<Project>() {
          @Override
          public Predicate toPredicate(
              javax.persistence.criteria.Root<Project> root,
              javax.persistence.criteria.CriteriaQuery<?> q,
              javax.persistence.criteria.CriteriaBuilder cb) {
            Predicate p = cb.isNotNull(root.get("endDate"));
            p = cb.and(p, cb.lessThan(root.<LocalDate>get("endDate"), today));
            p = cb.and(p, cb.not(root.get("status").in(CLOSED_PROJECT_STATUSES)));
            if (ownerUserId != null) {
              p = cb.and(p, cb.equal(root.get("ownerUserId"), ownerUserId));
            }
            return p;
          }
        };
    List<Project> rows = projectRepo.findAll(spec);
    List<OverdueProjectRow> out = new ArrayList<OverdueProjectRow>(rows.size());
    for (Project p : rows) {
      OverdueProjectRow r = new OverdueProjectRow();
      r.setProjectId(p.getId());
      r.setCode(p.getCode());
      r.setName(p.getName());
      r.setStatus(p.getStatus());
      r.setOwnerUserId(p.getOwnerUserId());
      r.setExpectedEndDate(p.getEndDate());
      r.setDaysOverdue(ChronoUnit.DAYS.between(p.getEndDate(), today));
      out.add(r);
    }
    return out;
  }

  private long countOpportunities(
      Instant periodStart, Instant periodEnd, Long ownerUserId, String status) {
    Specification<Opportunity> spec =
        new Specification<Opportunity>() {
          @Override
          public Predicate toPredicate(
              javax.persistence.criteria.Root<Opportunity> root,
              javax.persistence.criteria.CriteriaQuery<?> q,
              javax.persistence.criteria.CriteriaBuilder cb) {
            Predicate p = cb.greaterThanOrEqualTo(root.<Instant>get("createTime"), periodStart);
            p = cb.and(p, cb.lessThan(root.<Instant>get("createTime"), periodEnd));
            if (status != null) {
              p = cb.and(p, cb.equal(root.get("status"), status));
            }
            if (ownerUserId != null) {
              p = cb.and(p, cb.equal(root.get("commercialOwnerUserId"), ownerUserId));
            }
            return p;
          }
        };
    return oppRepo.count(spec);
  }
}
