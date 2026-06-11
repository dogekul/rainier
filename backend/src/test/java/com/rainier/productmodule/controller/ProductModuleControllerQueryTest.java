/* (C) 2026 Rainier — internal use only. */
package com.rainier.productmodule.controller;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
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

/** TC-PMOD-019..021 — detail field set (parent + path) and parentId / productId filters. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductModuleControllerQueryTest {

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

  private Long postModule(String code, String name, Long productId, Long parentId)
      throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", code);
    body.put("name", name);
    body.put("productId", productId);
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

  /** TC-PMOD-019: GET 详情字段集含 parent + path 字段，pathName 两段. */
  @Test
  void get_childModule_returnsParentAndPathFields() throws Exception {
    Long parent = postModule("MOD-WALLET", "钱包", pid, null);
    Long id = postModule("MOD-BALANCE", "余额", pid, parent);
    MvcResult res =
        mockMvc.perform(get("/api/product-modules/" + id)).andExpect(status().isOk()).andReturn();
    JsonNode body =
        json.readTree(
            res.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    String[] expected = {
      "id", "code", "name", "description", "status",
      "productId", "productCode", "productName",
      "parentId", "parentCode", "parentName", "pathName", "pathCodes",
      "ownerUserId", "ownerName", "ownerLoginName",
      "createTime", "updateTime", "createBy", "updateBy"
    };
    for (String f : expected) {
      org.junit.jupiter.api.Assertions.assertTrue(body.has(f), "expected field: " + f);
    }
    org.junit.jupiter.api.Assertions.assertEquals("钱包 / 余额", body.get("pathName").asText());
    org.junit.jupiter.api.Assertions.assertEquals(
        "MOD-WALLET / MOD-BALANCE", body.get("pathCodes").asText());
  }

  /** TC-PMOD-020: 按 parentId 过滤列表. */
  @Test
  void getList_filterByParentId_returnsOnlyChildren() throws Exception {
    Long m1 = postModule("MOD-M1", "根一", pid, null);
    postModule("MOD-M2", "子一", pid, m1);
    postModule("MOD-M3", "子二", pid, m1);
    postModule("MOD-M4", "根二", pid, null);
    mockMvc
        .perform(get("/api/product-modules?parentId=" + m1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.content[*].parentId", everyItem(is(m1.intValue()))));
  }

  /** TC-PMOD-021: 按 productId 过滤（返回全树 5 个，含子孙）. */
  @Test
  void getList_filterByProductId_returnsWholeTree() throws Exception {
    Long otherPid = createProduct("PROD-OTHER", "另一产品");
    Long root = postModule("MOD-R", "根", pid, null);
    Long mid = postModule("MOD-M", "中", pid, root);
    postModule("MOD-L", "叶", pid, mid);
    postModule("MOD-R2", "根二", pid, null);
    postModule("MOD-R3", "根三", pid, null);
    postModule("MOD-FOREIGN", "异", otherPid, null);
    mockMvc
        .perform(get("/api/product-modules?productId=" + pid + "&size=50"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(5))
        .andExpect(jsonPath("$.content[*].productId", everyItem(is(pid.intValue()))));
  }
}
