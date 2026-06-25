/* (C) 2026 Rainier — internal use only. */
package com.rainier.operationissue.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.operation.domain.Operation;
import com.rainier.operation.repository.OperationRepository;
import com.rainier.operationissue.domain.IssueSeverity;
import com.rainier.operationissue.domain.IssueStatus;
import com.rainier.operationissue.domain.OperationIssue;
import com.rainier.operationissue.repository.OperationIssueRepository;
import com.rainier.project.domain.Project;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.task.repository.TaskRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * v0.0.95 — D7: 分页 /api/operations/{id}/issues/page + 转工单 /api/operation-issues/{id}/convert-to-task.
 * Covers TC-OPI-PAGE-001/002 + TC-OPI-CONV-001/002/003.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OperationIssuePaginationConvertTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper json;
  @Autowired private OperationRepository opRepo;
  @Autowired private OperationIssueRepository issueRepo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private TaskRepository taskRepo;

  @BeforeEach
  void cleanDb() {
    taskRepo.deleteAll();
    issueRepo.deleteAll();
    opRepo.deleteAll();
    projectRepo.deleteAll();
    userRepo.deleteAll();
  }

  private Long seedUser(String name) {
    User u = new User();
    u.setLoginName(name);
    u.setName(name);
    u.setIsInternal(true);
    u.setEnabled(true);
    return userRepo.saveAndFlush(u).getId();
  }

  private Long seedOp() {
    Operation o = new Operation();
    o.setCustomerName("C");
    o.setTitle("Op-1");
    return opRepo.saveAndFlush(o).getId();
  }

  private Long seedProject(String code) {
    Project p = new Project();
    p.setCode(code);
    p.setName("Proj " + code);
    p.setStatus("ACTIVE");
    p.setOwnerUserId(seedUser("owner-" + code));
    p.setEnabled(true);
    return projectRepo.saveAndFlush(p).getId();
  }

  private Long seedIssue(Long opId, String title, String status, String severity, Long reporter) {
    OperationIssue i = new OperationIssue();
    i.setOperationId(opId);
    i.setTitle(title);
    i.setDescription("desc " + title);
    i.setStatus(status);
    i.setSeverity(severity);
    i.setReporterUserId(reporter);
    return issueRepo.saveAndFlush(i).getId();
  }

  /** TC-OPI-PAGE-001: 25 issues, page=0 size=10 → 10 items, total=25. */
  @Test
  void list_paged_returnsCorrectSlice() throws Exception {
    Long op = seedOp();
    Long alice = seedUser("alice");
    for (int n = 0; n < 25; n++) {
      seedIssue(op, "I-" + n, IssueStatus.OPEN, IssueSeverity.MEDIUM, alice);
    }
    mockMvc
        .perform(get("/api/operations/" + op + "/issues/page?page=0&size=10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(25))
        .andExpect(jsonPath("$.content.length()").value(10));
  }

  /** TC-OPI-PAGE-002: status filter returns only matching rows. */
  @Test
  void list_paged_statusFilter() throws Exception {
    Long op = seedOp();
    Long alice = seedUser("alice");
    seedIssue(op, "I-open-1", IssueStatus.OPEN, IssueSeverity.HIGH, alice);
    seedIssue(op, "I-open-2", IssueStatus.OPEN, IssueSeverity.LOW, alice);
    seedIssue(op, "I-closed-1", IssueStatus.CLOSED, IssueSeverity.LOW, alice);
    mockMvc
        .perform(get("/api/operations/" + op + "/issues/page?status=OPEN"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2));
  }

  /** TC-OPI-CONV-001: convert creates Task and flips issue status to CONVERTED. */
  @Test
  void convertToTask_createsTaskAndMarksConverted() throws Exception {
    Long op = seedOp();
    Long alice = seedUser("alice");
    Long proj = seedProject("P-CONV");
    Long issueId = seedIssue(op, "灯泡坏", IssueStatus.OPEN, IssueSeverity.HIGH, alice);

    ObjectNode body = json.createObjectNode();
    body.put("projectId", proj);
    mockMvc
        .perform(
            post("/api/operation-issues/" + issueId + "/convert-to-task")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("灯泡坏"))
        .andExpect(jsonPath("$.projectId").value(proj));

    // Issue is now CONVERTED
    mockMvc
        .perform(get("/api/operation-issues/" + issueId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CONVERTED"));
  }

  /** TC-OPI-CONV-002: invalid project → 400. */
  @Test
  void convertToTask_invalidProject_returns400() throws Exception {
    Long op = seedOp();
    Long alice = seedUser("alice");
    Long issueId = seedIssue(op, "X", IssueStatus.OPEN, IssueSeverity.LOW, alice);
    ObjectNode body = json.createObjectNode();
    body.put("projectId", 999999L);
    mockMvc
        .perform(
            post("/api/operation-issues/" + issueId + "/convert-to-task")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest());
  }

  /** TC-OPI-CONV-003: unknown issue → 404. */
  @Test
  void convertToTask_unknownIssue_returns404() throws Exception {
    Long proj = seedProject("P-X");
    ObjectNode body = json.createObjectNode();
    body.put("projectId", proj);
    mockMvc
        .perform(
            post("/api/operation-issues/999999/convert-to-task")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isNotFound());
  }
}
