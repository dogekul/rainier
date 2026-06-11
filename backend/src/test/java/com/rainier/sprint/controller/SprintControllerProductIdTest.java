/* (C) 2026 Rainier — internal use only. */
package com.rainier.sprint.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.product.domain.Product;
import com.rainier.product.domain.ProductStatus;
import com.rainier.product.repository.ProductRepository;
import com.rainier.common.domain.Priority;
import com.rainier.requirement.domain.Requirement;
import com.rainier.requirement.domain.RequirementStatus;
import com.rainier.requirement.repository.RequirementRepository;
import com.rainier.sprint.domain.Sprint;
import com.rainier.sprint.repository.SprintRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
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

/** TC-SPR-PF-001..004 — Sprint productId nullable / pre-bind / immutable / enrich. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SprintControllerProductIdTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private SprintRepository sprintRepo;
  @Autowired private ProductRepository productRepo;
  @Autowired private RequirementRepository requirementRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private ObjectMapper json;

  private Long uid;
  private Long reqId;

  @BeforeEach
  void seed() {
    sprintRepo.deleteAll();
    productRepo.deleteAll();
    requirementRepo.deleteAll();
    userRepo.deleteAll();
    User u = new User();
    u.setLoginName("alice");
    u.setName("Alice");
    u.setIsInternal(true);
    u.setEnabled(true);
    uid = userRepo.saveAndFlush(u).getId();
    Requirement r = new Requirement();
    r.setCode("REQ-PF");
    r.setTitle("需求");
    r.setOwnerUserId(uid);
    r.setStatus(RequirementStatus.DRAFT);
    r.setPriority(Priority.MEDIUM);
    reqId = requirementRepo.saveAndFlush(r).getId();
  }

  private Long createProduct(String code, String name) {
    Product p = new Product();
    p.setCode(code);
    p.setName(name);
    p.setStatus(ProductStatus.ACTIVE);
    p.setOwnerUserId(uid);
    return productRepo.saveAndFlush(p).getId();
  }

  /** TC-SPR-PF-001: 创建 Sprint 不传 productId → productId 为 null. */
  @Test
  void post_withoutProductId_productIdNull() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", "SPR-N1");
    body.put("name", "迭代");
    body.put("requirementId", reqId);
    body.put("ownerUserId", uid);
    mockMvc
        .perform(
            post("/api/sprints").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.productId").value(org.hamcrest.Matchers.nullValue()));
  }

  /** TC-SPR-PF-002: 创建 Sprint 预绑 productId. */
  @Test
  void post_withProductId_bindsAndEnrichesName() throws Exception {
    Long pid = createProduct("PROD-PF", "支付平台");
    ObjectNode body = json.createObjectNode();
    body.put("code", "SPR-N2");
    body.put("name", "迭代");
    body.put("requirementId", reqId);
    body.put("productId", pid);
    body.put("ownerUserId", uid);
    MvcResult res =
        mockMvc
            .perform(
                post("/api/sprints")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body.toString()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.productId").value(pid))
            .andReturn();
    String json8 = res.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    Assertions.assertTrue(json8.contains("支付平台"), "productName should be enriched: " + json8);
  }

  /** TC-SPR-PF-002b: productId 不存在 → 400. */
  @Test
  void post_unknownProductId_returns400() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", "SPR-N2B");
    body.put("name", "迭代");
    body.put("requirementId", reqId);
    body.put("productId", 999999L);
    body.put("ownerUserId", uid);
    mockMvc
        .perform(
            post("/api/sprints").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", org.hamcrest.Matchers.startsWith("product not found")));
  }

  /** TC-SPR-PF-003: 更新 Sprint 携带 productId 被静默忽略（不可变）. */
  @Test
  void put_withProductId_silentlyIgnored() throws Exception {
    Long pid = createProduct("PROD-IMM", "原产品");
    Long pid2 = createProduct("PROD-OTHER", "另一产品");
    Sprint s = new Sprint();
    s.setCode("SPR-IMM");
    s.setName("迭代");
    s.setStatus(com.rainier.sprint.domain.SprintStatus.PLANNING);
    s.setRequirementId(reqId);
    s.setProductId(pid);
    s.setOwnerUserId(uid);
    Long sId = sprintRepo.saveAndFlush(s).getId();

    ObjectNode body = json.createObjectNode();
    body.put("code", "SPR-IMM");
    body.put("name", "迭代改");
    body.put("status", "ACTIVE");
    body.put("ownerUserId", uid);
    body.put("productId", pid2); // attempt to switch — must be ignored
    mockMvc
        .perform(
            put("/api/sprints/" + sId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk());
    Sprint fromDb = sprintRepo.findById(sId).orElseThrow(IllegalStateException::new);
    Assertions.assertEquals(pid, fromDb.getProductId(), "productId must stay immutable");
  }

  /** TC-SPR-PF-004: GET 详情含 productId + productName. */
  @Test
  void get_detail_includesProductIdAndName() throws Exception {
    Long pid = createProduct("PROD-GET", "金融产品");
    Sprint s = new Sprint();
    s.setCode("SPR-GET");
    s.setName("迭代");
    s.setStatus(com.rainier.sprint.domain.SprintStatus.PLANNING);
    s.setRequirementId(reqId);
    s.setProductId(pid);
    s.setOwnerUserId(uid);
    Long sId = sprintRepo.saveAndFlush(s).getId();

    MvcResult res =
        mockMvc
            .perform(get("/api/sprints/" + sId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.productId").value(pid))
            .andReturn();
    String json8 = res.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    Assertions.assertTrue(json8.contains("金融产品"), "productName should be enriched: " + json8);
  }
}
