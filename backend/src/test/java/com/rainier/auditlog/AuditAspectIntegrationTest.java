/* (C) 2026 Rainier — internal use only. */
package com.rainier.auditlog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.auditlog.domain.AuditAction;
import com.rainier.auditlog.domain.AuditLog;
import com.rainier.auditlog.repository.AuditLogRepository;
import com.rainier.feature.domain.Feature;
import com.rainier.feature.domain.FeatureStatus;
import com.rainier.feature.repository.FeatureRepository;
import com.rainier.product.domain.Product;
import com.rainier.product.domain.ProductStatus;
import com.rainier.product.repository.ProductRepository;
import com.rainier.productmodule.domain.ProductModule;
import com.rainier.productmodule.domain.ProductModuleStatus;
import com.rainier.productmodule.repository.ProductModuleRepository;
import com.rainier.requirement.dto.RequirementCreateRequest;
import com.rainier.requirement.repository.RequirementRepository;
import com.rainier.requirement.service.RequirementService;
import com.rainier.sprint.domain.Sprint;
import com.rainier.sprint.domain.SprintStatus;
import com.rainier.sprint.repository.SprintRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

/** TC-AUD-001..009 — the audit aspect woven over real business writes. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditAspectIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AuditLogRepository auditRepo;
  @Autowired private RequirementRepository requirementRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private ProductRepository productRepo;
  @Autowired private ProductModuleRepository moduleRepo;
  @Autowired private FeatureRepository featureRepo;
  @Autowired private SprintRepository sprintRepo;
  @Autowired private RequirementService requirementService;
  @Autowired private TransactionTemplate txTemplate;
  @Autowired private ObjectMapper json;

  private Long uid;

  @BeforeEach
  void seed() {
    auditRepo.deleteAll();
    featureRepo.deleteAll();
    moduleRepo.deleteAll();
    productRepo.deleteAll();
    sprintRepo.deleteAll();
    requirementRepo.deleteAll();
    userRepo.deleteAll();
    User u = new User();
    u.setLoginName("alice");
    u.setName("Alice");
    u.setIsInternal(true);
    u.setEnabled(true);
    uid = userRepo.saveAndFlush(u).getId();
  }

  private ObjectNode reqBody(String code) {
    ObjectNode body = json.createObjectNode();
    body.put("code", code);
    body.put("title", "需求");
    body.put("ownerUserId", uid);
    body.put("priority", "MEDIUM");
    return body;
  }

  private Long postRequirement(String code) throws Exception {
    MvcResult res =
        mockMvc
            .perform(
                post("/api/requirements")
                    .requestAttr("username", "alice")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(reqBody(code).toString()))
            .andExpect(status().isCreated())
            .andReturn();
    return json.readTree(res.getResponse().getContentAsString()).get("id").asLong();
  }

  private List<AuditLog> auditFor(String entityType, Long entityId) {
    return auditRepo.findAll().stream()
        .filter(a -> a.getEntityType().equals(entityType) && a.getEntityId().equals(entityId))
        .collect(java.util.stream.Collectors.toList());
  }

  /** TC-AUD-001: 创建实体产生 CREATE 审计行 + actor. */
  @Test
  void create_producesCreateAuditRowWithActor() throws Exception {
    Long id = postRequirement("REQ-A1");
    List<AuditLog> rows = auditFor("REQUIREMENT", id);
    Assertions.assertEquals(1, rows.size());
    AuditLog a = rows.get(0);
    Assertions.assertEquals(AuditAction.CREATE, a.getAction());
    Assertions.assertEquals("REQUIREMENT", a.getEntityType());
    Assertions.assertEquals(id, a.getEntityId());
    Assertions.assertEquals("alice", a.getActor());
  }

  /** TC-AUD-004: summary 文本格式. */
  @Test
  void create_summaryFormat() throws Exception {
    Long id = postRequirement("REQ-SUM");
    AuditLog a = auditFor("REQUIREMENT", id).get(0);
    Assertions.assertEquals("CREATE REQUIREMENT#" + id, a.getSummary());
  }

  /** TC-AUD-002: 更新实体产生 UPDATE 审计行. */
  @Test
  void update_producesUpdateAuditRow() throws Exception {
    Long id = postRequirement("REQ-U1");
    ObjectNode body = reqBody("REQ-U1");
    body.put("title", "改后");
    mockMvc
        .perform(
            put("/api/requirements/" + id)
                .requestAttr("username", "alice")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk());
    List<AuditLog> updates =
        auditFor("REQUIREMENT", id).stream()
            .filter(a -> a.getAction().equals(AuditAction.UPDATE))
            .collect(java.util.stream.Collectors.toList());
    Assertions.assertEquals(1, updates.size());
    Assertions.assertEquals(id, updates.get(0).getEntityId());
  }

  /** TC-AUD-003: 删除实体产生 DELETE 审计行. */
  @Test
  void delete_producesDeleteAuditRow() throws Exception {
    Long id = postRequirement("REQ-D1");
    mockMvc
        .perform(delete("/api/requirements/" + id).requestAttr("username", "alice"))
        .andExpect(status().isNoContent());
    List<AuditLog> deletes =
        auditFor("REQUIREMENT", id).stream()
            .filter(a -> a.getAction().equals(AuditAction.DELETE))
            .collect(java.util.stream.Collectors.toList());
    Assertions.assertEquals(1, deletes.size());
    Assertions.assertEquals(id, deletes.get(0).getEntityId());
  }

  /** TC-AUD-005: 业务校验失败不记审计. */
  @Test
  void failedWrite_noAuditRow() throws Exception {
    postRequirement("REQ-DUP");
    long before = auditRepo.count();
    mockMvc
        .perform(
            post("/api/requirements")
                .requestAttr("username", "alice")
                .contentType(MediaType.APPLICATION_JSON)
                .content(reqBody("REQ-DUP").toString()))
        .andExpect(status().isConflict());
    Assertions.assertEquals(before, auditRepo.count(), "failed write must not add an audit row");
  }

  /**
   * TC-AUD-006: 审计写与业务同事务 —— 审计行在事务内可见(证明加入了同一事务)，回滚后与业务行一并消失。
   *
   * <p>关键: 仅断言"回滚后审计行不存在"是重言式(从未写入也满足)。这里先在事务内断言审计行 +1
   * (证明它确实写进了这个事务)，再断言回滚后归零 —— 二者配对才区分"加入事务并回滚" vs "根本没写"。
   */
  @Test
  void rollback_auditRowRolledBackWithBusiness() {
    long beforeAudit = auditRepo.count();
    long beforeReq = requirementRepo.count();
    RequirementCreateRequest req = new RequirementCreateRequest();
    req.setCode("REQ-RB");
    req.setTitle("回滚");
    req.setOwnerUserId(uid);
    req.setPriority("MEDIUM");
    long[] inTxAuditCount = new long[1];
    long[] inTxReqCount = new long[1];
    txTemplate.execute(
        status -> {
          requirementService.create(req); // writes business + audit in this tx
          // Both rows are visible within the still-open transaction → the audit write joined it.
          inTxAuditCount[0] = auditRepo.count();
          inTxReqCount[0] = requirementRepo.count();
          status.setRollbackOnly(); // force rollback
          return null;
        });
    Assertions.assertEquals(
        beforeAudit + 1, inTxAuditCount[0], "audit row must exist INSIDE the tx (joined it)");
    Assertions.assertEquals(beforeReq + 1, inTxReqCount[0], "business row exists inside the tx");
    Assertions.assertEquals(beforeReq, requirementRepo.count(), "business row must roll back");
    Assertions.assertEquals(
        beforeAudit, auditRepo.count(), "audit row must roll back with business (same tx)");
  }

  /** TC-AUD-007: 复合名 service → SPRINT_FEATURE_LINK. */
  @Test
  void compoundServiceName_entityTypeSnakeCase() throws Exception {
    Product p = new Product();
    p.setCode("PROD-AUD");
    p.setName("产品");
    p.setStatus(ProductStatus.ACTIVE);
    p.setOwnerUserId(uid);
    Long pid = productRepo.saveAndFlush(p).getId();
    ProductModule m = new ProductModule();
    m.setCode("MOD-AUD");
    m.setName("模块");
    m.setStatus(ProductModuleStatus.ACTIVE);
    m.setProductId(pid);
    m.setOwnerUserId(uid);
    Long mid = moduleRepo.saveAndFlush(m).getId();
    Feature f = new Feature();
    f.setCode("FEAT-AUD");
    f.setName("功能");
    f.setStatus(FeatureStatus.PLANNING);
    f.setModuleId(mid);
    f.setOwnerUserId(uid);
    Long fid = featureRepo.saveAndFlush(f).getId();
    com.rainier.requirement.domain.Requirement r = new com.rainier.requirement.domain.Requirement();
    r.setCode("REQ-SF-AUD");
    r.setTitle("需求");
    r.setOwnerUserId(uid);
    r.setStatus(com.rainier.requirement.domain.RequirementStatus.DRAFT);
    r.setPriority(com.rainier.common.domain.Priority.MEDIUM);
    Long rid = requirementRepo.saveAndFlush(r).getId();
    Sprint s = new Sprint();
    s.setCode("SPR-AUD");
    s.setName("迭代");
    s.setStatus(SprintStatus.PLANNING);
    s.setRequirementId(rid);
    s.setOwnerUserId(uid);
    Long sid = sprintRepo.saveAndFlush(s).getId();

    ObjectNode body = json.createObjectNode();
    body.put("sprintId", sid);
    body.put("featureId", fid);
    mockMvc
        .perform(
            post("/api/sprint-features")
                .requestAttr("username", "alice")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated());
    long matched =
        auditRepo.findAll().stream()
            .filter(a -> "SPRINT_FEATURE_LINK".equals(a.getEntityType()))
            .filter(a -> AuditAction.CREATE.equals(a.getAction()))
            .count();
    Assertions.assertEquals(1, matched, "expected one SPRINT_FEATURE_LINK CREATE audit row");
  }

  /** TC-AUD-008: 查询审计不自增审计（防自递归）. */
  @Test
  void queryingAudit_doesNotProduceAudit() throws Exception {
    postRequirement("REQ-Q1");
    long k = auditRepo.count();
    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                "/api/audit-logs"))
        .andExpect(status().isOk());
    Assertions.assertEquals(k, auditRepo.count(), "querying audit must not add audit rows");
  }

  /** TC-AUD-009: 多 service 各产生审计行，entityType 各异. */
  @Test
  void multipleServices_eachAudited() throws Exception {
    // requirement
    postRequirement("REQ-MULTI");
    // product
    ObjectNode prod = json.createObjectNode();
    prod.put("code", "PROD-MULTI");
    prod.put("name", "产品");
    prod.put("ownerUserId", uid);
    mockMvc
        .perform(
            post("/api/products")
                .requestAttr("username", "alice")
                .contentType(MediaType.APPLICATION_JSON)
                .content(prod.toString()))
        .andExpect(status().isCreated());
    // Each distinct service produces EXACTLY one audit row with the correct entityType
    // (catches mislabeling or double-audit, the 🔴 AOP-weaving sampling guard).
    long reqRows =
        auditRepo.findAll().stream().filter(a -> "REQUIREMENT".equals(a.getEntityType())).count();
    long prodRows =
        auditRepo.findAll().stream().filter(a -> "PRODUCT".equals(a.getEntityType())).count();
    Assertions.assertEquals(1, reqRows, "exactly one REQUIREMENT audit row");
    Assertions.assertEquals(1, prodRows, "exactly one PRODUCT audit row");
  }
}
