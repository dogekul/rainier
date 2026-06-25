/* (C) 2026 Rainier — internal use only. */
package com.rainier.organizationpmo.controller;

import com.rainier.common.authz.AuthzService;
import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.ForbiddenException;
import com.rainier.organization.domain.Organization;
import com.rainier.organization.repository.OrganizationRepository;
import com.rainier.organizationpmo.domain.OrganizationPmo;
import com.rainier.organizationpmo.dto.EffectivePmoDetail;
import com.rainier.organizationpmo.dto.OrganizationPmoCreateRequest;
import com.rainier.organizationpmo.dto.OrganizationPmoDetail;
import com.rainier.organizationpmo.service.OrganizationPmoService;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * v0.0.64 — Organization↔PMO 关系 REST 接口。
 *
 * <ul>
 *   <li>GET / 任何登录用户
 *   <li>POST/DELETE / admin only
 *   <li>delete 时检查 inherited PMO → 400「请到上级组织 XX 操作」
 * </ul>
 */
@RestController
@RequestMapping("/api/organizations")
public class OrganizationPmoController {

  private final OrganizationPmoService service;
  private final OrganizationRepository orgRepo;
  private final AuthzService authz;

  public OrganizationPmoController(
      OrganizationPmoService service, OrganizationRepository orgRepo, AuthzService authz) {
    this.service = service;
    this.orgRepo = orgRepo;
    this.authz = authz;
  }

  /** Own PMOs of this organization (NOT inherited). */
  @GetMapping("/{id}/pmos")
  public List<OrganizationPmoDetail> list(@PathVariable Long id) {
    return service.query(id);
  }

  /** Effective PMOs = own UNION 沿 parent_id 向上的祖先 PMOs（去重 + 按层级排序）。 */
  @GetMapping("/{id}/effective-pmos")
  public List<EffectivePmoDetail> effectivePmos(@PathVariable Long id) {
    return service.findEffectivePmos(id);
  }

  @PostMapping("/{id}/pmos")
  @ResponseStatus(HttpStatus.CREATED)
  public OrganizationPmoDetail add(
      @PathVariable Long id,
      @Valid @RequestBody OrganizationPmoCreateRequest req,
      HttpServletRequest httpReq) {
    if (!authz.isCurrentUserAdmin(httpReq)) {
      throw new ForbiddenException("仅 admin 可管理组织 PMO");
    }
    return service.create(id, req.getUserId());
  }

  /**
   * Delete a PMO. Path: organizationId + userId — controller resolves the row, then verifies it
   * belongs to that org (not inherited). admin only.
   */
  @DeleteMapping("/{id}/pmos/{userId}")
  public void remove(
      @PathVariable Long id, @PathVariable Long userId, HttpServletRequest httpReq) {
    if (!authz.isCurrentUserAdmin(httpReq)) {
      throw new ForbiddenException("仅 admin 可管理组织 PMO");
    }
    // Resolve the row scoped to this org. If user is a PMO of an ANCESTOR (inherited), there's no
    // own row → reject with the "请到上级组织 XX 操作" message.
    List<OrganizationPmoDetail> own = service.query(id);
    OrganizationPmoDetail target = null;
    for (OrganizationPmoDetail d : own) {
      if (userId.equals(d.getUserId())) {
        target = d;
        break;
      }
    }
    if (target == null) {
      // Check if inherited
      List<EffectivePmoDetail> effective = service.findEffectivePmos(id);
      for (EffectivePmoDetail e : effective) {
        if (userId.equals(e.getUserId()) && !id.equals(e.getInheritedFromOrgId())) {
          Organization parent = orgRepo.findById(e.getInheritedFromOrgId()).orElse(null);
          String name = parent == null ? "上级" : parent.getName();
          throw new BadRequestException("请到上级组织 " + name + " 操作");
        }
      }
      throw new BadRequestException("该 PMO 不存在于本组织");
    }
    service.delete(target.getId());
  }
}
