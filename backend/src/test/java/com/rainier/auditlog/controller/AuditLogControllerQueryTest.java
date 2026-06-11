/* (C) 2026 Rainier — internal use only. */
package com.rainier.auditlog.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rainier.auditlog.domain.AuditAction;
import com.rainier.auditlog.domain.AuditLog;
import com.rainier.auditlog.repository.AuditLogRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** TC-AUD-010..014 — audit-log read API. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditLogControllerQueryTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private AuditLogRepository repo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void clean() {
    repo.deleteAll();
  }

  private Long seed(String actor, String entityType, Long entityId, String action) {
    AuditLog a = new AuditLog();
    a.setActor(actor);
    a.setEntityType(entityType);
    a.setEntityId(entityId);
    a.setAction(action);
    a.setSummary(action + " " + entityType + "#" + entityId);
    return repo.saveAndFlush(a).getId();
  }

  /** TC-AUD-010: 按 entityType + entityId 过滤. */
  @Test
  void query_filterByEntityTypeAndId() throws Exception {
    seed("alice", "REQUIREMENT", 5L, AuditAction.CREATE);
    seed("alice", "REQUIREMENT", 5L, AuditAction.UPDATE);
    seed("alice", "REQUIREMENT", 9L, AuditAction.CREATE);
    seed("alice", "PRODUCT", 5L, AuditAction.CREATE);
    mockMvc
        .perform(get("/api/audit-logs?entityType=REQUIREMENT&entityId=5"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(
            jsonPath(
                "$.content[*].entityType",
                org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("REQUIREMENT"))))
        .andExpect(
            jsonPath(
                "$.content[*].entityId",
                org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(5))));
  }

  /** TC-AUD-011: 按 actor 过滤. */
  @Test
  void query_filterByActor() throws Exception {
    seed("alice", "REQUIREMENT", 1L, AuditAction.CREATE);
    seed("alice", "PRODUCT", 2L, AuditAction.CREATE);
    seed("alice", "SPRINT", 3L, AuditAction.CREATE);
    seed("system", "ROLE", 4L, AuditAction.CREATE);
    mockMvc
        .perform(get("/api/audit-logs?actor=alice"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(3))
        .andExpect(
            jsonPath(
                "$.content[*].actor",
                org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("alice"))));
  }

  /** TC-AUD-012: 倒序（最新在前）. */
  @Test
  void query_descendingNewestFirst() throws Exception {
    Long a = seed("alice", "REQUIREMENT", 1L, AuditAction.CREATE); // A (older)
    Long b = seed("alice", "REQUIREMENT", 2L, AuditAction.CREATE); // B (newer)
    MvcResult res =
        mockMvc.perform(get("/api/audit-logs")).andExpect(status().isOk()).andReturn();
    JsonNode content =
        json.readTree(res.getResponse().getContentAsString()).get("content");
    // Full order, not just [0]: newest (B) first, oldest (A) last.
    Assertions.assertEquals(b.intValue(), content.get(0).get("id").asInt(), "newest first");
    Assertions.assertEquals(a.intValue(), content.get(1).get("id").asInt(), "oldest last");
  }

  /** TC-AUD-013: 单条查询字段集. */
  @Test
  void getById_fieldSet() throws Exception {
    Long id = seed("alice", "REQUIREMENT", 7L, AuditAction.UPDATE);
    MvcResult res =
        mockMvc.perform(get("/api/audit-logs/" + id)).andExpect(status().isOk()).andReturn();
    JsonNode body =
        json.readTree(
            res.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    String[] expected = {
      "id", "actor", "entityType", "entityId", "action", "summary", "createTime"
    };
    for (String f : expected) {
      Assertions.assertTrue(body.has(f), "expected field: " + f);
    }
    // Values, not just presence — catches a field-swap / wrong-row bug.
    Assertions.assertEquals(id.intValue(), body.get("id").asInt());
    Assertions.assertEquals("alice", body.get("actor").asText());
    Assertions.assertEquals("REQUIREMENT", body.get("entityType").asText());
    Assertions.assertEquals(7, body.get("entityId").asInt());
    Assertions.assertEquals("UPDATE", body.get("action").asText());
  }

  /**
   * TC-AUD-014: append-only — no write mapping in the controller, and POST writes nothing.
   * (POST surfaces as 500 not 405 because the pre-existing catch-all handler maps the
   * unhandled HttpRequestMethodNotSupportedException to 500 — see pending-adjustments PA. The
   * load-bearing guarantees are the absent write mappings + zero persisted rows.)
   */
  @Test
  void noWriteEndpoints() throws Exception {
    long before = repo.count();
    mockMvc
        .perform(post("/api/audit-logs").contentType("application/json").content("{}"))
        .andExpect(result -> {
          int s = result.getResponse().getStatus();
          Assertions.assertTrue(s < 200 || s >= 300, "POST must not succeed; got " + s);
        });
    Assertions.assertEquals(before, repo.count(), "POST must not persist an audit row");

    String src =
        new String(
            java.nio.file.Files.readAllBytes(
                java.nio.file.Paths.get(
                    "src/main/java/com/rainier/auditlog/controller/AuditLogController.java")),
            java.nio.charset.StandardCharsets.UTF_8);
    Assertions.assertFalse(src.contains("@PostMapping"), "no @PostMapping");
    Assertions.assertFalse(src.contains("@PutMapping"), "no @PutMapping");
    Assertions.assertFalse(src.contains("@DeleteMapping"), "no @DeleteMapping");
  }
}
