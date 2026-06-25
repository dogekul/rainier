/* (C) 2026 Rainier — internal use only. */
package com.rainier.organization.controller;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rainier.auditlog.domain.AuditAction;
import com.rainier.auditlog.domain.AuditLog;
import com.rainier.auditlog.repository.AuditLogRepository;
import com.rainier.organization.domain.Organization;
import com.rainier.organization.domain.OrganizationType;
import com.rainier.organization.repository.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * v0.0.99 (E3) — GET /api/organizations/{id}/audit-log.
 *
 * <p>验证：
 *
 * <ul>
 *   <li>仅返回 entityType=ORGANIZATION + entityId=id 的行（不漏不串）
 *   <li>action 过滤
 *   <li>不存在的组织 → 404
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrganizationAuditEndpointTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AuditLogRepository auditRepo;
  @Autowired private OrganizationRepository orgRepo;

  @BeforeEach
  void clean() {
    auditRepo.deleteAll();
    orgRepo.deleteAll();
  }

  private Long createOrg(String code, String name) {
    Organization o = new Organization();
    o.setType(OrganizationType.COMPANY);
    o.setCode(code);
    o.setName(name);
    o.setPath("/0");
    o.setWholeName(name);
    o.setEnabled(true);
    return orgRepo.saveAndFlush(o).getId();
  }

  private void seedAudit(String entityType, Long entityId, String action) {
    AuditLog a = new AuditLog();
    a.setActor("alice");
    a.setEntityType(entityType);
    a.setEntityId(entityId);
    a.setAction(action);
    a.setSummary(action + " " + entityType + "#" + entityId);
    auditRepo.saveAndFlush(a);
  }

  /** Scenario: 查询某组织的审计历史只返回该组织的 ORGANIZATION 行. */
  @Test
  void auditLog_filtersByEntityTypeAndId() throws Exception {
    Long o100 = createOrg("O100", "O100");
    Long o200 = createOrg("O200", "O200");
    seedAudit("ORGANIZATION", o100, AuditAction.UPDATE);
    seedAudit("ORGANIZATION", o100, AuditAction.CREATE);
    seedAudit("ORGANIZATION", o200, AuditAction.UPDATE); // 另一个组织 — 不应出现
    seedAudit("REQUIREMENT", o100, AuditAction.UPDATE); // 同 id 不同 entityType — 不应出现

    mockMvc
        .perform(get("/api/organizations/" + o100 + "/audit-log"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.content[*].entityType", everyItem(is("ORGANIZATION"))))
        .andExpect(jsonPath("$.content[*].entityId", everyItem(is(o100.intValue()))));
  }

  /** Scenario: action 过滤. */
  @Test
  void auditLog_filtersByAction() throws Exception {
    Long o100 = createOrg("O100", "O100");
    seedAudit("ORGANIZATION", o100, AuditAction.CREATE);
    seedAudit("ORGANIZATION", o100, AuditAction.UPDATE);
    seedAudit("ORGANIZATION", o100, AuditAction.UPDATE);

    mockMvc
        .perform(get("/api/organizations/" + o100 + "/audit-log?action=UPDATE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.content[*].action", everyItem(is("UPDATE"))));
  }

  /** Scenario: 不存在的组织 → 404. */
  @Test
  void auditLog_unknownOrg_returns404() throws Exception {
    mockMvc
        .perform(get("/api/organizations/9999999/audit-log"))
        .andExpect(status().isNotFound());
  }
}
