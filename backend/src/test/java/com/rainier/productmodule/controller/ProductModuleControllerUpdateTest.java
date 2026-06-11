/* (C) 2026 Rainier — internal use only. */
package com.rainier.productmodule.controller;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

/** TC-PMOD-013..018 — reparent (cross-product → cycle → depth) + status/owner update. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductModuleControllerUpdateTest {

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
    Product p = new Product();
    p.setCode("PROD-PAY");
    p.setName("支付平台");
    p.setStatus(ProductStatus.ACTIVE);
    p.setOwnerUserId(uid);
    pid = productRepo.saveAndFlush(p).getId();
  }

  private Long createUser(String loginName, String name) {
    User u = new User();
    u.setLoginName(loginName);
    u.setName(name);
    u.setIsInternal(true);
    u.setEnabled(true);
    return userRepo.saveAndFlush(u).getId();
  }

  private Long postModule(String code, String name, Long parentId) throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", code);
    body.put("name", name);
    body.put("productId", pid);
    if (parentId != null) {
      body.put("parentId", parentId);
    }
    body.put("ownerUserId", uid);
    MvcResult res =
        mockMvc
            .perform(
                post("/api/product-modules")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body.toString()))
            .andExpect(status().isCreated())
            .andReturn();
    return json.readTree(res.getResponse().getContentAsString()).get("id").asLong();
  }

  /** Full-replace PUT body; parentId nullable (null moves to top level). */
  private ObjectNode putBody(String code, String name, Long parentId, Long ownerUserId) {
    ObjectNode body = json.createObjectNode();
    body.put("code", code);
    body.put("name", name);
    body.put("status", "PLANNING");
    if (parentId != null) {
      body.put("parentId", parentId);
    }
    body.put("ownerUserId", ownerUserId);
    return body;
  }

  /** TC-PMOD-013: 同产品内 reparent 成功 → 200 + parentId 变 + path 重拼. */
  @Test
  void put_reparentWithinProduct_returns200WithNewPath() throws Exception {
    Long oldParent = postModule("MOD-OLD", "旧根", null);
    Long newParent = postModule("MOD-NEW", "新根", null);
    Long id = postModule("MOD-MOVE", "搬家", oldParent);
    mockMvc
        .perform(
            put("/api/product-modules/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(putBody("MOD-MOVE", "搬家", newParent, uid).toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.parentId").value(newParent))
        .andExpect(jsonPath("$.pathName").value("新根 / 搬家"));
  }

  /** TC-PMOD-014: reparent 到自身 → 400 "cycle detected". */
  @Test
  void put_reparentToSelf_returns400Cycle() throws Exception {
    Long id = postModule("MOD-SELF", "自指", null);
    mockMvc
        .perform(
            put("/api/product-modules/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(putBody("MOD-SELF", "自指", id, uid).toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("cycle detected")));
  }

  /** TC-PMOD-015: reparent 到真子孙 → 400 "cycle detected"（A→B→C 后把 A 挂到 C）. */
  @Test
  void put_reparentToDescendant_returns400Cycle() throws Exception {
    Long a = postModule("MOD-A", "甲", null);
    Long b = postModule("MOD-B", "乙", a);
    Long c = postModule("MOD-C", "丙", b);
    mockMvc
        .perform(
            put("/api/product-modules/" + a)
                .contentType(MediaType.APPLICATION_JSON)
                .content(putBody("MOD-A", "甲", c, uid).toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("cycle detected")));
  }

  /** TC-PMOD-016: reparent 后子树最深超 max → 400（X→Y 挂到 A→B 的 B 下，Y 落第 4 层）. */
  @Test
  void put_reparentExceedingDepth_returns400() throws Exception {
    Long a = postModule("MOD-A", "甲", null);
    Long b = postModule("MOD-B", "乙", a);
    Long x = postModule("MOD-X", "丁", null);
    postModule("MOD-Y", "戊", x); // X 子树高度 2
    mockMvc
        .perform(
            put("/api/product-modules/" + x)
                .contentType(MediaType.APPLICATION_JSON)
                .content(putBody("MOD-X", "丁", b, uid).toString()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message", startsWith("max module depth exceeded")));
  }

  /** TC-PMOD-017: reparent 到 null（省略 parentId = 移为顶层）→ 200 单段 path. */
  @Test
  void put_reparentToNull_movesToTopLevel() throws Exception {
    Long parent = postModule("MOD-ROOT", "根", null);
    Long id = postModule("MOD-CHILD", "子", parent);
    mockMvc
        .perform(
            put("/api/product-modules/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(putBody("MOD-CHILD", "子", null, uid).toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.parentId").value(org.hamcrest.Matchers.nullValue()))
        .andExpect(jsonPath("$.pathName").value("子"));
  }

  /** TC-PMOD-018: 更新 status + owner 转移. */
  @Test
  void put_updateStatusAndOwner_returns200() throws Exception {
    Long lili = createUser("lili", "黎立");
    Long id = postModule("MOD-UPD", "升级", null);
    ObjectNode body = putBody("MOD-UPD", "升级", null, lili);
    body.put("status", "ACTIVE");
    mockMvc
        .perform(
            put("/api/product-modules/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.ownerName").value("黎立"))
        .andExpect(jsonPath("$.ownerLoginName").value("lili"));
  }
}
