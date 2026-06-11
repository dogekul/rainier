/* (C) 2026 Rainier — internal use only. */
package com.rainier.productmodule.controller;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.product.domain.Product;
import com.rainier.product.domain.ProductStatus;
import com.rainier.product.repository.ProductRepository;
import com.rainier.productmodule.repository.ProductModuleRepository;
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

/** TC-PMOD-001..012 — create with optional parentId, composite code unique, depth cap (max=3). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductModuleControllerCreateTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ProductModuleRepository repo;
  @Autowired private ProductRepository productRepo;
  @Autowired private UserRepository userRepo;
  @Autowired private ObjectMapper json;

  private Long uid;
  private Long pid;

  @BeforeEach
  void seed() {
    repo.deleteAll();
    productRepo.deleteAll();
    userRepo.deleteAll();
    User u = new User();
    u.setLoginName("alice");
    u.setName("Alice");
    u.setIsInternal(true);
    u.setEnabled(true);
    uid = userRepo.saveAndFlush(u).getId();
    pid = createProduct("PROD-PAY", "支付平台");
  }

  private Long createProduct(String code, String name) {
    Product p = new Product();
    p.setCode(code);
    p.setName(name);
    p.setStatus(ProductStatus.ACTIVE);
    p.setOwnerUserId(uid);
    return productRepo.saveAndFlush(p).getId();
  }

  private ObjectNode moduleBody(String code, String name, Long productId, Long parentId) {
    ObjectNode body = json.createObjectNode();
    body.put("code", code);
    body.put("name", name);
    body.put("productId", productId);
    if (parentId != null) {
      body.put("parentId", parentId);
    }
    body.put("ownerUserId", uid);
    return body;
  }

  private Long postModule(String code, String name, Long productId, Long parentId)
      throws Exception {
    MvcResult res =
        mockMvc
            .perform(
                post("/api/product-modules")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(moduleBody(code, name, productId, parentId).toString()))
            .andExpect(status().isCreated())
            .andReturn();
    return json.readTree(res.getResponse().getContentAsString()).get("id").asLong();
  }

  /** TC-PMOD-001: 顶层创建（无 parentId）→ 201, parentId=null, path=自己. */
  @Test
  void post_topLevel_returns201WithSingleSegmentPath() throws Exception {
    mockMvc
        .perform(
            post("/api/product-modules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(moduleBody("MOD-WALLET", "钱包", pid, null).toString()))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", matchesPattern("/api/product-modules/\\d+")))
        .andExpect(jsonPath("$.parentId").value(org.hamcrest.Matchers.nullValue()))
        .andExpect(jsonPath("$.pathName").value("钱包"))
        .andExpect(jsonPath("$.pathCodes").value("MOD-WALLET"))
        .andExpect(jsonPath("$.status").value("PLANNING"));
  }

  /** TC-PMOD-002: 子模块创建 → 201, pathName="钱包 / 余额". */
  @Test
  void post_childUnderTopLevel_returns201WithJoinedPath() throws Exception {
    Long parentId = postModule("MOD-WALLET", "钱包", pid, null);
    mockMvc
        .perform(
            post("/api/product-modules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(moduleBody("MOD-BALANCE", "余额", pid, parentId).toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.parentId").value(parentId))
        .andExpect(jsonPath("$.parentCode").value("MOD-WALLET"))
        .andExpect(jsonPath("$.parentName").value("钱包"))
        .andExpect(jsonPath("$.pathName").value("钱包 / 余额"))
        .andExpect(jsonPath("$.pathCodes").value("MOD-WALLET / MOD-BALANCE"));
  }

  /** TC-PMOD-003: parentId 不存在 → 400 "parent module not found". */
  @Test
  void post_unknownParent_returns400() throws Exception {
    mockMvc
        .perform(
            post("/api/product-modules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(moduleBody("MOD-X", "x", pid, 999_999L).toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("parent module not found")));
  }

  /** TC-PMOD-004: productId 不存在 → 400 "product not found". */
  @Test
  void post_unknownProduct_returns400() throws Exception {
    mockMvc
        .perform(
            post("/api/product-modules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(moduleBody("MOD-X", "x", 999_999L, null).toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("product not found")));
  }

  /** TC-PMOD-005: invalid status → 400. */
  @Test
  void post_invalidStatus_returns400() throws Exception {
    ObjectNode body = moduleBody("MOD-X", "x", pid, null);
    body.put("status", "UNKNOWN");
    mockMvc
        .perform(
            post("/api/product-modules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("invalid status")));
  }

  /** TC-PMOD-006: missing required → 400 fieldErrors name/productId/ownerUserId. */
  @Test
  void post_missingRequired_returns400() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", "MOD-MISS");
    mockMvc
        .perform(
            post("/api/product-modules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[*].field", org.hamcrest.Matchers.hasItem("name")))
        .andExpect(jsonPath("$.fieldErrors[*].field", org.hamcrest.Matchers.hasItem("productId")))
        .andExpect(
            jsonPath("$.fieldErrors[*].field", org.hamcrest.Matchers.hasItem("ownerUserId")));
  }

  /** TC-PMOD-007: 同 parent 下 code 重复 → 409 "code already exists under parent". */
  @Test
  void post_duplicateCodeUnderSameParent_returns409() throws Exception {
    Long parentId = postModule("MOD-TOP", "顶", pid, null);
    postModule("MOD-DUP", "一号", pid, parentId);
    mockMvc
        .perform(
            post("/api/product-modules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(moduleBody("MOD-DUP", "二号", pid, parentId).toString()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", startsWith("code already exists under parent")));
  }

  /** TC-PMOD-008: 不同 parent 下 code 可重复 → 两次 201. */
  @Test
  void post_sameCodeUnderDifferentParents_bothSucceed() throws Exception {
    Long parentA = postModule("MOD-PA", "甲", pid, null);
    Long parentB = postModule("MOD-PB", "乙", pid, null);
    postModule("MOD-CFG", "配置甲", pid, parentA);
    mockMvc
        .perform(
            post("/api/product-modules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(moduleBody("MOD-CFG", "配置乙", pid, parentB).toString()))
        .andExpect(status().isCreated());
  }

  /** TC-PMOD-009: 同 product 顶层 code 重复 → 409 "code already exists in top-level"（应用层）. */
  @Test
  void post_duplicateTopLevelCodeSameProduct_returns409() throws Exception {
    postModule("MOD-TOP", "顶一", pid, null);
    mockMvc
        .perform(
            post("/api/product-modules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(moduleBody("MOD-TOP", "顶二", pid, null).toString()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", startsWith("code already exists in top-level")));
  }

  /** TC-PMOD-010: parent 属另一 product → 400 "parent module must belong to the same product". */
  @Test
  void post_crossProductParent_returns400() throws Exception {
    Long otherPid = createProduct("PROD-OTHER", "另一产品");
    Long foreignParent = postModule("MOD-FOREIGN", "异", otherPid, null);
    mockMvc
        .perform(
            post("/api/product-modules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(moduleBody("MOD-X", "x", pid, foreignParent).toString()))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message", startsWith("parent module must belong to the same product")));
  }

  /** TC-PMOD-011: 创建第 4 层（max=3）→ 400 "max module depth exceeded: 4 > 3". */
  @Test
  void post_fourthLevel_returns400DepthExceeded() throws Exception {
    Long l1 = postModule("MOD-L1", "一层", pid, null);
    Long l2 = postModule("MOD-L2", "二层", pid, l1);
    Long l3 = postModule("MOD-L3", "三层", pid, l2);
    mockMvc
        .perform(
            post("/api/product-modules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(moduleBody("MOD-L4", "四层", pid, l3).toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("max module depth exceeded: 4 > 3"));
  }

  /** TC-PMOD-012: 创建第 3 层（等于 max=3）→ 201 + 三段 path. */
  @Test
  void post_thirdLevel_returns201() throws Exception {
    Long l1 = postModule("MOD-L1", "一层", pid, null);
    Long l2 = postModule("MOD-L2", "二层", pid, l1);
    mockMvc
        .perform(
            post("/api/product-modules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(moduleBody("MOD-L3", "三层", pid, l2).toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.pathName").value("一层 / 二层 / 三层"));
  }
}
