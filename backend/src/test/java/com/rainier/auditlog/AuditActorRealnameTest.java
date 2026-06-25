/* (C) 2026 Rainier — internal use only. */
package com.rainier.auditlog;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.auth.RequestUserContext;
import com.rainier.auth.service.AuthService;
import com.rainier.auditlog.domain.AuditAction;
import com.rainier.auditlog.domain.AuditLog;
import com.rainier.auditlog.repository.AuditLogRepository;
import com.rainier.requirement.dto.RequirementCreateRequest;
import com.rainier.requirement.repository.RequirementRepository;
import com.rainier.requirement.service.RequirementService;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
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

/**
 * v0.0.79 (B6) — actor on the audit row reflects the real loginName parsed from the Bearer token
 * (via {@link RequestUserContext} set by {@code SecurityFilter}), and degrades to "system" when
 * there's no token / no HTTP context (background path).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditActorRealnameTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AuthService authService;
  @Autowired private AuditLogRepository auditRepo;
  @Autowired private RequirementRepository requirementRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private RequirementService requirementService;
  @Autowired private TransactionTemplate txTemplate;
  @Autowired private ObjectMapper json;

  private Long uid;

  @BeforeEach
  void seed() {
    auditRepo.deleteAll();
    requirementRepo.deleteAll();
    userRepo.deleteAll();
    User u = new User();
    u.setLoginName("bob");
    u.setName("Bob");
    u.setIsInternal(true);
    u.setEnabled(true);
    uid = userRepo.saveAndFlush(u).getId();
    // Guard: a stray ThreadLocal from a prior test must not bleed in.
    RequestUserContext.clear();
  }

  @AfterEach
  void cleanup() {
    RequestUserContext.clear();
  }

  private ObjectNode reqBody(String code) {
    ObjectNode body = json.createObjectNode();
    body.put("code", code);
    body.put("title", "需求");
    body.put("ownerUserId", uid);
    body.put("priority", "MEDIUM");
    return body;
  }

  /** AUDIT-ACTOR-REAL-001: 带 token 调用 → actor = 真实 loginName. */
  @Test
  void withValidBearerToken_actorIsRealLoginName() throws Exception {
    String token = authService.issueToken("bob");
    MvcResult res =
        mockMvc
            .perform(
                post("/api/requirements")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(reqBody("REQ-ACTOR-1").toString()))
            .andExpect(status().isCreated())
            .andReturn();
    Long id = json.readTree(res.getResponse().getContentAsString()).get("id").asLong();

    List<AuditLog> rows =
        auditRepo.findAll().stream()
            .filter(a -> "REQUIREMENT".equals(a.getEntityType()))
            .filter(a -> id.equals(a.getEntityId()))
            .collect(Collectors.toList());
    Assertions.assertEquals(1, rows.size(), "expected exactly one REQUIREMENT CREATE row");
    AuditLog row = rows.get(0);
    Assertions.assertEquals(AuditAction.CREATE, row.getAction());
    Assertions.assertEquals("bob", row.getActor(), "actor must be the real loginName from token");
  }

  /** AUDIT-ACTOR-REAL-002: 后台路径（无 HTTP 上下文、无 ThreadLocal）→ actor = "system". */
  @Test
  void noHttpContext_actorDegradesToSystem() {
    RequirementCreateRequest req = new RequirementCreateRequest();
    req.setCode("REQ-ACTOR-SYS");
    req.setTitle("后台");
    req.setOwnerUserId(uid);
    req.setPriority("MEDIUM");
    Long[] idHolder = new Long[1];
    txTemplate.execute(
        status -> {
          idHolder[0] = requirementService.create(req).getId();
          return null;
        });
    List<AuditLog> rows =
        auditRepo.findAll().stream()
            .filter(a -> "REQUIREMENT".equals(a.getEntityType()))
            .filter(a -> idHolder[0].equals(a.getEntityId()))
            .collect(Collectors.toList());
    Assertions.assertEquals(1, rows.size());
    Assertions.assertEquals(
        "system", rows.get(0).getActor(), "actor must degrade to system without identity");
  }

  /** AUDIT-ACTOR-REAL-003: SecurityFilter 末端 clear()，请求结束后 ThreadLocal 不残留. */
  @Test
  void afterRequest_threadLocalIsCleared() throws Exception {
    String token = authService.issueToken("bob");
    mockMvc
        .perform(
            post("/api/requirements")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(reqBody("REQ-ACTOR-CLEAR").toString()))
        .andExpect(status().isCreated());
    // MockMvc runs the filter on the test thread; after dispatch, the filter's finally must have
    // cleared the ThreadLocal so the next request on a recycled thread sees no identity.
    Assertions.assertNull(
        RequestUserContext.get(), "SecurityFilter must clear ThreadLocal at end of request");
  }
}
