/* (C) 2026 Rainier — internal use only. */
package com.rainier.me.service;

import com.rainier.common.domain.Priority;
import com.rainier.demand.domain.Demand;
import com.rainier.demand.domain.DemandStatus;
import com.rainier.demand.repository.DemandRepository;
import com.rainier.demandrequirement.domain.DemandRequirementLink;
import com.rainier.demandrequirement.repository.DemandRequirementLinkRepository;
import com.rainier.me.dto.InboxResponse;
import com.rainier.project.domain.Project;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.requirement.domain.Requirement;
import com.rainier.requirement.repository.RequirementRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PO 需求收件箱 read-model (v0.0.42): unconverted demands (no requirement link, non-terminal) awaiting
 * triage + the caller's own requirements. Self-scoped, all-users. Pure read aggregation — zero writes.
 * Degrades to an empty inbox when the token subject has no matching User.
 */
@Service
@Transactional(readOnly = true)
public class MeInboxService {

  private static final List<String> PRIORITY_ORDER =
      Arrays.asList(Priority.URGENT, Priority.HIGH, Priority.MEDIUM, Priority.LOW, Priority.LOWEST);

  /** Terminal demand states — a never-converted DONE/CLOSED demand is dead, not a triage item. */
  private static final Set<String> TERMINAL_DEMAND =
      Collections.unmodifiableSet(new HashSet<>(Arrays.asList(DemandStatus.DONE, DemandStatus.CLOSED)));

  private final UserRepository userRepo;
  private final DemandRepository demandRepo;
  private final DemandRequirementLinkRepository linkRepo;
  private final RequirementRepository requirementRepo;
  private final ProjectRepository projectRepo;

  public MeInboxService(
      UserRepository userRepo,
      DemandRepository demandRepo,
      DemandRequirementLinkRepository linkRepo,
      RequirementRepository requirementRepo,
      ProjectRepository projectRepo) {
    this.userRepo = userRepo;
    this.demandRepo = demandRepo;
    this.linkRepo = linkRepo;
    this.requirementRepo = requirementRepo;
    this.projectRepo = projectRepo;
  }

  public InboxResponse inbox(String username) {
    InboxResponse out = new InboxResponse();
    User me = userRepo.findByLoginName(username).orElse(null);
    if (me == null) {
      return out; // degraded: both sections empty
    }
    out.setUnconvertedDemands(unconvertedDemands());
    out.setMyRequirements(myRequirements(me.getId()));
    return out;
  }

  private List<InboxResponse.InboxDemand> unconvertedDemands() {
    Set<Long> linked =
        linkRepo.findAll().stream()
            .map(DemandRequirementLink::getDemandId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    return demandRepo.findAll().stream()
        .filter(d -> !linked.contains(d.getId()))
        .filter(d -> !TERMINAL_DEMAND.contains(d.getStatus()))
        .sorted(
            Comparator.comparingInt((Demand d) -> priorityRank(d.getPriority()))
                .thenComparing(Demand::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder())))
        .map(
            d ->
                new InboxResponse.InboxDemand(
                    d.getId(), d.getTitle(), d.getPriority(), d.getStatus(), d.getCreateTime()))
        .collect(Collectors.toList());
  }

  private List<InboxResponse.InboxRequirement> myRequirements(Long ownerUserId) {
    List<Requirement> reqs = requirementRepo.findByOwnerUserId(ownerUserId);
    Set<Long> projectIds =
        reqs.stream().map(Requirement::getProjectId).filter(Objects::nonNull).collect(Collectors.toSet());
    Map<Long, Project> projectMap = new HashMap<>();
    if (!projectIds.isEmpty()) {
      projectRepo.findAllById(projectIds).forEach(p -> projectMap.put(p.getId(), p));
    }
    return reqs.stream()
        .sorted(
            Comparator.comparingInt((Requirement r) -> priorityRank(r.getPriority()))
                .thenComparing(
                    Requirement::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder())))
        .map(
            r -> {
              Project p = r.getProjectId() == null ? null : projectMap.get(r.getProjectId());
              return new InboxResponse.InboxRequirement(
                  r.getId(),
                  r.getCode(),
                  r.getTitle(),
                  r.getStatus(),
                  r.getPriority(),
                  r.getExpectedDate(),
                  r.getProjectId(),
                  p == null ? null : p.getName());
            })
        .collect(Collectors.toList());
  }

  private static int priorityRank(String priority) {
    int idx = PRIORITY_ORDER.indexOf(priority);
    return idx < 0 ? PRIORITY_ORDER.size() : idx;
  }
}
