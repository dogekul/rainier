/* (C) 2026 Rainier — internal use only. */
package com.rainier.organizationpmo.service;

import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.ConflictException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.organization.domain.Organization;
import com.rainier.organization.repository.OrganizationRepository;
import com.rainier.organization.service.OrganizationService;
import com.rainier.organizationpmo.domain.OrganizationPmo;
import com.rainier.organizationpmo.dto.EffectivePmoDetail;
import com.rainier.organizationpmo.dto.OrganizationPmoDetail;
import com.rainier.organizationpmo.repository.OrganizationPmoRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * v0.0.64 — Organization↔PMO 多对多关系业务。
 *
 * <ul>
 *   <li>create(orgId, userId) — admin 操作（controller 层校验）。注册后 AuditAspect 自动捕获。
 *   <li>delete(id) — admin 操作；不可删 inherited PMO（controller 层先校验 id 来自 own）。
 *   <li>query(orgId) — 列 own PMOs（不含继承）；任何登录用户可读。
 *   <li>findEffectivePmos(orgId) — own + 继承的全部 PMOs；任何登录用户可读。
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class OrganizationPmoService {

  private final OrganizationPmoRepository repo;
  private final OrganizationRepository orgRepo;
  private final OrganizationService orgService;
  private final UserRepository userRepo;

  public OrganizationPmoService(
      OrganizationPmoRepository repo,
      OrganizationRepository orgRepo,
      OrganizationService orgService,
      UserRepository userRepo) {
    this.repo = repo;
    this.orgRepo = orgRepo;
    this.orgService = orgService;
    this.userRepo = userRepo;
  }

  @Transactional
  public OrganizationPmoDetail create(Long organizationId, Long userId) {
    if (!orgRepo.existsById(organizationId)) {
      throw new BadRequestException("organization not found: id=" + organizationId);
    }
    if (!userRepo.existsById(userId)) {
      throw new BadRequestException("user not found: id=" + userId);
    }
    if (repo.existsByOrganizationIdAndUserId(organizationId, userId)) {
      throw new ConflictException("PMO 已存在");
    }
    OrganizationPmo p = new OrganizationPmo();
    p.setOrganizationId(organizationId);
    p.setUserId(userId);
    OrganizationPmo saved = repo.saveAndFlush(p);
    return enrich(saved);
  }

  @Transactional
  public void delete(Long id) {
    OrganizationPmo p =
        repo.findById(id).orElseThrow(() -> new NotFoundException("PMO not found: id=" + id));
    repo.delete(p);
  }

  /** v0.0.64 — query own PMOs of a specific organization (NOT inherited). */
  public List<OrganizationPmoDetail> query(Long organizationId) {
    List<OrganizationPmo> rows = repo.findByOrganizationIdOrderById(organizationId);
    if (rows.isEmpty()) return java.util.Collections.emptyList();
    return enrichAll(rows);
  }

  /** v0.0.64 — query effective PMOs = own UNION 沿 parent_id 链向上的祖先 PMOs，去重保留首条来源。 */
  public List<EffectivePmoDetail> findEffectivePmos(Long organizationId) {
    List<Long> ancestorIds = orgService.getAncestorIds(organizationId);
    if (ancestorIds.isEmpty()) return java.util.Collections.emptyList();
    List<OrganizationPmo> rows = repo.findByOrganizationIdInOrderById(ancestorIds);
    if (rows.isEmpty()) return java.util.Collections.emptyList();

    // userIds + orgIds for enrich
    Set<Long> userIds = new HashSet<Long>();
    Set<Long> orgIds = new HashSet<Long>();
    for (OrganizationPmo r : rows) {
      userIds.add(r.getUserId());
      orgIds.add(r.getOrganizationId());
    }
    Map<Long, User> users = userRepo.findAllById(userIds).stream()
        .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
    Map<Long, Organization> orgs = orgService.findAllById(orgIds);

    // Order: own 先 (orgId == organizationId)；继承的按 ancestor 顺序 (深度优先).
    // ancestorIds 顺序 = [self, parent, grandparent, ...]
    Map<Long, Integer> rank = new HashMap<Long, Integer>();
    for (int i = 0; i < ancestorIds.size(); i++) {
      rank.put(ancestorIds.get(i), i);
    }

    // 去重：同一 user 出现在多个 ancestor 时，保留最近的（rank 最小的）来源
    Map<Long, EffectivePmoDetail> dedup = new HashMap<Long, EffectivePmoDetail>();
    for (OrganizationPmo r : rows) {
      Integer thisRank = rank.get(r.getOrganizationId());
      if (thisRank == null) continue;
      EffectivePmoDetail existing = dedup.get(r.getUserId());
      if (existing != null) {
        Integer existingRank = rank.get(existing.getInheritedFromOrgId());
        if (existingRank != null && existingRank <= thisRank) continue;
      }
      EffectivePmoDetail d = new EffectivePmoDetail();
      d.setUserId(r.getUserId());
      User u = users.get(r.getUserId());
      if (u != null) {
        d.setUserName(u.getName());
        d.setUserLoginName(u.getLoginName());
      }
      d.setInheritedFromOrgId(r.getOrganizationId());
      Organization o = orgs.get(r.getOrganizationId());
      if (o != null) {
        d.setInheritedFromOrgName(o.getName());
      }
      dedup.put(r.getUserId(), d);
    }

    // Sort by rank ascending (self 0 → root last).
    List<EffectivePmoDetail> result = new ArrayList<EffectivePmoDetail>(dedup.values());
    result.sort((a, b) -> {
      Integer ra = rank.get(a.getInheritedFromOrgId());
      Integer rb = rank.get(b.getInheritedFromOrgId());
      return Integer.compare(ra == null ? Integer.MAX_VALUE : ra, rb == null ? Integer.MAX_VALUE : rb);
    });
    return result;
  }

  /**
   * v0.0.64 — 校验试图在子 org 上删除 inherited PMO。controller 调 delete 前应 fetch by id 然后 verify
   * `pmo.organizationId == requestPathOrgId`；若不等则该 PMO 是 inherited，应返回 400。
   */
  public OrganizationPmo getOrThrow(Long id) {
    return repo.findById(id)
        .orElseThrow(() -> new NotFoundException("PMO not found: id=" + id));
  }

  private OrganizationPmoDetail enrich(OrganizationPmo p) {
    OrganizationPmoDetail d = OrganizationPmoDetail.from(p);
    userRepo.findById(p.getUserId()).ifPresent(u -> {
      d.setUserName(u.getName());
      d.setUserLoginName(u.getLoginName());
    });
    orgRepo.findById(p.getOrganizationId()).ifPresent(o -> d.setOrganizationName(o.getName()));
    return d;
  }

  private List<OrganizationPmoDetail> enrichAll(List<OrganizationPmo> rows) {
    Set<Long> userIds = new HashSet<Long>();
    Set<Long> orgIds = new HashSet<Long>();
    for (OrganizationPmo r : rows) {
      userIds.add(r.getUserId());
      orgIds.add(r.getOrganizationId());
    }
    Map<Long, User> users = userRepo.findAllById(userIds).stream()
        .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
    Map<Long, Organization> orgs = orgService.findAllById(orgIds);

    List<OrganizationPmoDetail> result = new ArrayList<OrganizationPmoDetail>(rows.size());
    for (OrganizationPmo r : rows) {
      OrganizationPmoDetail d = OrganizationPmoDetail.from(r);
      User u = users.get(r.getUserId());
      if (u != null) {
        d.setUserName(u.getName());
        d.setUserLoginName(u.getLoginName());
      }
      Organization o = orgs.get(r.getOrganizationId());
      if (o != null) {
        d.setOrganizationName(o.getName());
      }
      result.add(d);
    }
    return result;
  }
}
