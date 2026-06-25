/* (C) 2026 Rainier — internal use only. */
package com.rainier.requirementfeature.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.common.domain.Priority;
import com.rainier.feature.domain.Feature;
import com.rainier.feature.domain.FeatureStatus;
import com.rainier.feature.repository.FeatureRepository;
import com.rainier.product.domain.Product;
import com.rainier.product.repository.ProductRepository;
import com.rainier.productmodule.domain.ProductModule;
import com.rainier.productmodule.repository.ProductModuleRepository;
import com.rainier.requirement.domain.Requirement;
import com.rainier.requirement.domain.RequirementStatus;
import com.rainier.requirement.repository.RequirementRepository;
import com.rainier.requirementfeature.repository.RequirementFeatureLinkRepository;
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

/** Integration tests for {@link RequirementFeatureLinkController}. Covers TC-RFL-001..008. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RequirementFeatureLinkControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private RequirementFeatureLinkRepository linkRepo;
  @Autowired private RequirementRepository reqRepo;
  @Autowired private FeatureRepository featureRepo;
  @Autowired private ProductModuleRepository moduleRepo;
  @Autowired private ProductRepository productRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private ObjectMapper json;

  private Long userId;
  private Long moduleId;

  @BeforeEach
  void cleanDb() {
    linkRepo.deleteAll();
    featureRepo.deleteAll();
    moduleRepo.deleteAll();
    productRepo.deleteAll();
    reqRepo.deleteAll();
    userRepo.deleteAll();
    userId = createUser("rfl-user");
    Long productId = createProduct(userId);
    moduleId = createModule(productId, userId, "RFL-MOD");
  }

  private Long createUser(String login) {
    User u = new User();
    u.setLoginName(login);
    u.setName(login);
    u.setIsInternal(true);
    u.setEnabled(true);
    return userRepo.saveAndFlush(u).getId();
  }

  private Long createProduct(Long ownerId) {
    Product p = new Product();
    p.setCode("RFL-PROD-" + System.nanoTime());
    p.setName("rfl-prod");
    p.setStatus("ACTIVE");
    p.setOwnerUserId(ownerId);
    return productRepo.saveAndFlush(p).getId();
  }

  private Long createModule(Long productId, Long ownerId, String code) {
    ProductModule m = new ProductModule();
    m.setCode(code + "-" + System.nanoTime());
    m.setName("mod");
    m.setStatus("ACTIVE");
    m.setProductId(productId);
    m.setOwnerUserId(ownerId);
    return moduleRepo.saveAndFlush(m).getId();
  }

  private Long createReq(String code) {
    Requirement r = new Requirement();
    r.setCode(code);
    r.setTitle("rfl");
    r.setDescription("rfl");
    r.setOwnerUserId(userId);
    r.setStatus(RequirementStatus.DRAFT);
    r.setPriority(Priority.MEDIUM);
    return reqRepo.saveAndFlush(r).getId();
  }

  private Long createFeature(String code) {
    Feature f = new Feature();
    f.setCode(code);
    f.setName("f");
    f.setDescription("d");
    f.setStatus(FeatureStatus.PLANNING);
    f.setModuleId(moduleId);
    f.setOwnerUserId(userId);
    return featureRepo.saveAndFlush(f).getId();
  }

  private Long postLink(Long requirementId, Long featureId) throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("requirementId", requirementId);
    body.put("featureId", featureId);
    MvcResult res =
        mockMvc
            .perform(
                post("/api/requirement-features")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body.toString()))
            .andExpect(status().isCreated())
            .andReturn();
    return json.readTree(res.getResponse().getContentAsString()).get("id").asLong();
  }

  /** TC-RFL-001 合法创建 */
  @Test
  void post_validLink_returns201() throws Exception {
    Long rid = createReq("REQ-RFL1");
    Long fid = createFeature("F-RFL1");
    ObjectNode body = json.createObjectNode();
    body.put("requirementId", rid);
    body.put("featureId", fid);
    mockMvc
        .perform(
            post("/api/requirement-features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.requirementId").value(rid))
        .andExpect(jsonPath("$.featureId").value(fid))
        .andExpect(jsonPath("$.linkedAt").isNotEmpty());
  }

  /** TC-RFL-002 唯一约束 */
  @Test
  void post_duplicate_returns409() throws Exception {
    Long rid = createReq("REQ-RFL2");
    Long fid = createFeature("F-RFL2");
    postLink(rid, fid);
    ObjectNode body = json.createObjectNode();
    body.put("requirementId", rid);
    body.put("featureId", fid);
    mockMvc
        .perform(
            post("/api/requirement-features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", startsWith("link already exists")));
  }

  /** TC-RFL-003 未知 requirementId */
  @Test
  void post_unknownRequirement_returns400() throws Exception {
    Long fid = createFeature("F-RFL3");
    ObjectNode body = json.createObjectNode();
    body.put("requirementId", 999_999L);
    body.put("featureId", fid);
    mockMvc
        .perform(
            post("/api/requirement-features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("requirement not found")));
  }

  /** TC-RFL-004 未知 featureId */
  @Test
  void post_unknownFeature_returns400() throws Exception {
    Long rid = createReq("REQ-RFL4");
    ObjectNode body = json.createObjectNode();
    body.put("requirementId", rid);
    body.put("featureId", 999_999L);
    mockMvc
        .perform(
            post("/api/requirement-features")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("feature not found")));
  }

  /** TC-RFL-005 反查 */
  @Test
  void reverseLookup_endpointsReturnLink() throws Exception {
    Long rid = createReq("REQ-RFL5");
    Long fid = createFeature("F-RFL5");
    postLink(rid, fid);
    mockMvc
        .perform(get("/api/requirements/" + rid + "/linked-features"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].featureId").value(fid));
    mockMvc
        .perform(get("/api/features/" + fid + "/requirements"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].requirementId").value(rid));
  }

  /** TC-RFL-006 硬删 */
  @Test
  void delete_returnsNoContentAndRowVanishes() throws Exception {
    Long rid = createReq("REQ-RFL6");
    Long fid = createFeature("F-RFL6");
    Long linkId = postLink(rid, fid);
    mockMvc.perform(delete("/api/requirement-features/" + linkId)).andExpect(status().isNoContent());
    mockMvc
        .perform(get("/api/requirements/" + rid + "/linked-features"))
        .andExpect(jsonPath("$", hasSize(0)));
    org.junit.jupiter.api.Assertions.assertEquals(0, linkRepo.count());
  }

  /** TC-RFL-007 RequirementDetail.featureIds enrichment */
  @Test
  void requirementDetail_enrichedWithFeatureIds() throws Exception {
    Long rid = createReq("REQ-RFL7");
    Long f1 = createFeature("F-RFL7-A");
    Long f2 = createFeature("F-RFL7-B");
    postLink(rid, f1);
    postLink(rid, f2);
    mockMvc
        .perform(get("/api/requirements/" + rid))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.featureIds", hasSize(2)));
  }

  /** TC-RFL-008 FeatureDetail.requirementIds batched enrichment via list endpoint */
  @Test
  void featureList_enrichedWithRequirementIds() throws Exception {
    Long fid = createFeature("F-RFL8");
    Long r1 = createReq("REQ-RFL8-A");
    Long r2 = createReq("REQ-RFL8-B");
    postLink(r1, fid);
    postLink(r2, fid);
    mockMvc
        .perform(get("/api/features?moduleId=" + moduleId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[?(@.id==" + fid + ")].requirementIds[*]", hasSize(2)));
  }
}
