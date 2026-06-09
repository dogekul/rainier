/* (C) 2026 Rainier — internal use only. */
package com.rainier.task.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.common.domain.Priority;
import com.rainier.project.domain.Project;
import com.rainier.project.domain.ProjectStatus;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.requirement.domain.Requirement;
import com.rainier.requirement.domain.RequirementStatus;
import com.rainier.requirement.repository.RequirementRepository;
import com.rainier.sprint.domain.Sprint;
import com.rainier.sprint.domain.SprintStatus;
import com.rainier.sprint.repository.SprintRepository;
import com.rainier.story.domain.Story;
import com.rainier.story.domain.StoryStatus;
import com.rainier.story.repository.StoryRepository;
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
import org.springframework.test.web.servlet.MvcResult;

/** Integration tests for {@link TaskController} GET endpoints. Covers TC-TSK-010, TC-TSK-011. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskControllerQueryTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private TaskRepository taskRepo;
  @Autowired private StoryRepository storyRepo;
  @Autowired private SprintRepository sprintRepo;
  @Autowired private RequirementRepository requirementRepo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    taskRepo.deleteAll();
    storyRepo.deleteAll();
    sprintRepo.deleteAll();
    requirementRepo.deleteAll();
    projectRepo.deleteAll();
    userRepo.deleteAll();
  }

  private Long createUser(String loginName) {
    User u = new User();
    u.setLoginName(loginName);
    u.setName("U-" + loginName);
    u.setIsInternal(true);
    u.setEnabled(true);
    return userRepo.saveAndFlush(u).getId();
  }

  private Long createProject(Long ownerId, String code) {
    Project p = new Project();
    p.setCode(code);
    p.setName("Apollo");
    p.setStatus(ProjectStatus.ACTIVE);
    p.setOwnerUserId(ownerId);
    p.setEnabled(true);
    return projectRepo.saveAndFlush(p).getId();
  }

  private Long createRequirement(Long ownerId, Long projectId, String code) {
    Requirement r = new Requirement();
    r.setCode(code);
    r.setTitle("x");
    r.setOwnerUserId(ownerId);
    r.setProjectId(projectId);
    r.setStatus(RequirementStatus.DRAFT);
    r.setPriority(Priority.MEDIUM);
    return requirementRepo.saveAndFlush(r).getId();
  }

  private Long createSprint(Long reqId, Long ownerId, String code) {
    Sprint sp = new Sprint();
    sp.setCode(code);
    sp.setName("Phase");
    sp.setStatus(SprintStatus.PLANNING);
    sp.setRequirementId(reqId);
    sp.setOwnerUserId(ownerId);
    return sprintRepo.saveAndFlush(sp).getId();
  }

  private Long createStory(Long sprintId, Long projectId, Long ownerId, String code) {
    Story s = new Story();
    s.setCode(code);
    s.setTitle("登录页");
    s.setStatus(StoryStatus.DRAFT);
    s.setPriority(Priority.MEDIUM);
    s.setSprintId(sprintId);
    s.setProjectId(projectId);
    s.setOwnerUserId(ownerId);
    return storyRepo.saveAndFlush(s).getId();
  }

  private Long createTaskViaAPI(
      Long projectId, Long sprintId, Long storyId, Long assigneeUserId, String code, String status)
      throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", code);
    body.put("title", "T");
    body.put("projectId", projectId);
    if (sprintId != null) body.put("sprintId", sprintId);
    if (storyId != null) body.put("storyId", storyId);
    if (assigneeUserId != null) body.put("assigneeUserId", assigneeUserId);
    if (status != null) body.put("status", status);
    MvcResult res =
        mockMvc
            .perform(
                post("/api/tasks").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
            .andExpect(status().isCreated())
            .andReturn();
    return json.readTree(res.getResponse().getContentAsString()).get("id").asLong();
  }

  /** TC-TSK-010: GET 详情 23 字段全有. */
  @Test
  void get_existingId_returns23FieldDetailWithEnrichment() throws Exception {
    Long userId = createUser("alice");
    Long projectId = createProject(userId, "PROJ-Q1");
    Long reqId = createRequirement(userId, projectId, "REQ-Q1");
    Long sprintId = createSprint(reqId, userId, "SPR-Q1");
    Long storyId = createStory(sprintId, projectId, userId, "STR-Q1");
    Long id = createTaskViaAPI(projectId, sprintId, storyId, userId, "TASK-Q1", null);

    MvcResult res =
        mockMvc
            .perform(get("/api/tasks/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.projectName").value("Apollo"))
            .andReturn();
    JsonNode body = json.readTree(res.getResponse().getContentAsString());
    String[] expected = {
      "id",
      "code",
      "title",
      "description",
      "status",
      "priority",
      "projectId",
      "projectName",
      "projectCode",
      "sprintId",
      "sprintCode",
      "sprintName",
      "storyId",
      "storyCode",
      "storyTitle",
      "assigneeUserId",
      "assigneeName",
      "assigneeLoginName",
      "dueDate",
      "closeReason",
      "createTime",
      "updateTime",
      "createBy",
      "updateBy"
    };
    for (String f : expected) {
      org.junit.jupiter.api.Assertions.assertTrue(body.has(f), "expected field: " + f);
    }
  }

  /** TC-TSK-011: 按 projectId + status 联合过滤. */
  @Test
  void getList_filterByProjectIdAndStatus_returnsMatching() throws Exception {
    Long userId = createUser("alice");
    Long projectA = createProject(userId, "PROJ-A");
    Long projectB = createProject(userId, "PROJ-B");
    // 3 tasks under A (TODO / IN_PROGRESS / DONE), 1 under B (TODO)
    createTaskViaAPI(projectA, null, null, null, "TASK-A1", "TODO");
    createTaskViaAPI(projectA, null, null, null, "TASK-A2", "IN_PROGRESS");
    createTaskViaAPI(projectA, null, null, null, "TASK-A3", "DONE");
    createTaskViaAPI(projectB, null, null, null, "TASK-B1", "TODO");

    mockMvc
        .perform(get("/api/tasks?projectId=" + projectA + "&status=TODO"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.content[0].projectId").value(projectA))
        .andExpect(jsonPath("$.content[0].status", is("TODO")));
  }
}
