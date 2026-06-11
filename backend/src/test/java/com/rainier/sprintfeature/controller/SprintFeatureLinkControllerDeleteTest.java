/* (C) 2026 Rainier — internal use only. */
package com.rainier.sprintfeature.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rainier.sprint.domain.Sprint;
import com.rainier.sprint.domain.SprintStatus;
import com.rainier.sprint.repository.SprintRepository;
import com.rainier.sprintfeature.domain.SprintFeatureLink;
import com.rainier.sprintfeature.repository.SprintFeatureLinkRepository;
import com.rainier.common.domain.Priority;
import com.rainier.requirement.domain.Requirement;
import com.rainier.requirement.domain.RequirementStatus;
import com.rainier.requirement.repository.RequirementRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** TC-SF-007 (filter) + TC-SF-008 (hard delete) + TC-SF-009 (productId not rolled back). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SprintFeatureLinkControllerDeleteTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private SprintFeatureLinkRepository repo;
  @Autowired private SprintRepository sprintRepo;
  @Autowired private RequirementRepository requirementRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private ObjectMapper json;

  private Long uid;

  @BeforeEach
  void seed() {
    repo.deleteAll();
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

  private Long createSprint(String code, Long productId) {
    Requirement r = new Requirement();
    r.setCode("REQ-" + code);
    r.setTitle("需求");
    r.setOwnerUserId(uid);
    r.setStatus(RequirementStatus.DRAFT);
    r.setPriority(Priority.MEDIUM);
    Long reqId = requirementRepo.saveAndFlush(r).getId();
    Sprint s = new Sprint();
    s.setCode(code);
    s.setName("迭代");
    s.setStatus(SprintStatus.PLANNING);
    s.setRequirementId(reqId);
    s.setProductId(productId);
    s.setOwnerUserId(uid);
    return sprintRepo.saveAndFlush(s).getId();
  }

  private Long seedLink(Long sprintId, Long featureId) {
    SprintFeatureLink link = new SprintFeatureLink();
    link.setSprintId(sprintId);
    link.setFeatureId(featureId);
    return repo.saveAndFlush(link).getId();
  }

  /** TC-SF-007: 按 sprintId 过滤. */
  @Test
  void getList_filterBySprintId_returnsOnlyMatching() throws Exception {
    Long sA = createSprint("SPR-A", 100L);
    Long sB = createSprint("SPR-B", 200L);
    seedLink(sA, 11L);
    seedLink(sA, 12L);
    seedLink(sB, 13L);
    mockMvc
        .perform(get("/api/sprint-features?sprintId=" + sA))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.content[*].sprintId", org.hamcrest.Matchers.everyItem(
            org.hamcrest.Matchers.is(sA.intValue()))));
  }

  /** TC-SF-008: 硬删成功行物理消失. */
  @Test
  void delete_hardDeletes_rowPhysicallyGone() throws Exception {
    Long sId = createSprint("SPR-D", 100L);
    Long linkId = seedLink(sId, 11L);
    mockMvc.perform(delete("/api/sprint-features/" + linkId)).andExpect(status().isNoContent());
    Assertions.assertEquals(0, repo.findById(linkId).isPresent() ? 1 : 0, "row must be hard-deleted");
  }

  /** TC-SF-009: 解绑最后一个 feature 后 sprint.productId 不回退. */
  @Test
  void delete_lastFeature_sprintProductIdUnchanged() throws Exception {
    Long sId = createSprint("SPR-L", 100L);
    Long linkId = seedLink(sId, 11L);
    mockMvc.perform(delete("/api/sprint-features/" + linkId)).andExpect(status().isNoContent());
    Sprint s = sprintRepo.findById(sId).orElseThrow(IllegalStateException::new);
    Assertions.assertEquals(100L, s.getProductId(), "productId must not roll back to null");
  }
}
