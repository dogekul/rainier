/* (C) 2026 Rainier — internal use only. */
package com.rainier.organization.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.organization.repository.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for Organization read / update / move / delete endpoints. Covers
 * TC-ORG-006..016 + TC-MIG-003 (path /1/2/3 format).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrganizationControllerQueryTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private OrganizationRepository repo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    repo.deleteAll();
  }

  // ---------------------------- GET /{id} --------------------------------

  @Test
  void get_existingId_returns200WithFullDetail() throws Exception {
    Long id = createRoot("HQ", "总公司");
    mockMvc
        .perform(get("/api/organizations/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.type").value("COMPANY"))
        .andExpect(jsonPath("$.code").value("HQ"))
        .andExpect(jsonPath("$.name").value("总公司"))
        .andExpect(jsonPath("$.path").exists())
        .andExpect(jsonPath("$.wholeName").value("总公司"));
  }

  /** TC-RMP-002: GET /{id} response body 不含 isPmo 字段。 */
  @Test
  void get_byId_responseDoesNotContainIsPmo() throws Exception {
    Long id = createRoot("HQ", "总公司");
    mockMvc
        .perform(get("/api/organizations/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.isPmo").doesNotExist());
  }

  /**
   * TC-RMP-003: PUT body 中即便客户端发送残留 isPmo 字段，后端 SHALL 静默忽略（Jackson 默认 ignore-unknown=true）， 返回 200
   * + response 不含 isPmo。
   */
  @Test
  void put_withIsPmoInBody_silentlyIgnored_returns200() throws Exception {
    Long id = createRoot("HQ", "总公司");
    ObjectNode body = json.createObjectNode();
    body.put("code", "HQ");
    body.put("name", "新名");
    body.put("isPmo", true); // legacy field — must be silently ignored
    mockMvc
        .perform(
            put("/api/organizations/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("新名"))
        .andExpect(jsonPath("$.isPmo").doesNotExist());
  }

  @Test
  void get_softDeletedId_returns404() throws Exception {
    Long id = createRoot("X", "X");
    mockMvc.perform(delete("/api/organizations/" + id)).andExpect(status().isNoContent());
    mockMvc.perform(get("/api/organizations/" + id)).andExpect(status().isNotFound());
  }

  // ---------------------------- GET /tree --------------------------------

  @Test
  void getTree_excludesSoftDeletedAndSortsByPath() throws Exception {
    Long hq = createRoot("HQ", "总公司");
    Long rd = createChild(hq, "RD", "研发部", "DEPARTMENT");
    Long ops = createChild(hq, "OPS", "运维部", "DEPARTMENT");
    Long backend = createChild(rd, "BE", "后端组", "TEAM");
    // Soft-delete one
    mockMvc.perform(delete("/api/organizations/" + ops)).andExpect(status().isNoContent());

    MvcResult result =
        mockMvc
            .perform(get("/api/organizations/tree"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andReturn();
    String body = result.getResponse().getContentAsString();
    // Verify path ordering by checking ids appear in expected order
    assertThat(body.indexOf("\"id\":" + hq))
        .as("hq before rd")
        .isLessThan(body.indexOf("\"id\":" + rd));
    assertThat(body.indexOf("\"id\":" + rd))
        .as("rd before backend")
        .isLessThan(body.indexOf("\"id\":" + backend));
  }

  // ---------------------------- GET / (list) -----------------------------

  @Test
  void getList_filterByType_returnsOnlyMatchingType() throws Exception {
    Long hq = createRoot("HQ", "总公司");
    createChild(hq, "RD", "研发部", "DEPARTMENT");
    createChild(hq, "OPS", "运维部", "DEPARTMENT");
    createChild(hq, "PMO", "PMO 团队", "TEAM");
    mockMvc
        .perform(get("/api/organizations?type=DEPARTMENT"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.content[*].type", contains("DEPARTMENT", "DEPARTMENT")));
  }

  @Test
  void getList_searchMatchesWholeName() throws Exception {
    Long hq = createRoot("HQ", "总公司");
    createChild(hq, "RD", "研发部", "DEPARTMENT");
    createChild(hq, "OPS", "运维部", "DEPARTMENT");
    mockMvc
        .perform(get("/api/organizations?search=研发"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.content[0].wholeName").value("总公司/研发部"));
  }

  // ---------------------------- PUT /{id} update + cascade ---------------

  @Test
  void put_updateName_cascadesWholeNameToDescendants() throws Exception {
    Long hq = createRoot("HQ", "总公司");
    Long rd = createChild(hq, "RD", "研发部", "DEPARTMENT");
    Long be = createChild(rd, "BE", "后端组", "TEAM");

    ObjectNode body = json.createObjectNode();
    body.put("name", "研发中心");
    mockMvc
        .perform(
            put("/api/organizations/" + rd)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("研发中心"))
        .andExpect(jsonPath("$.wholeName").value("总公司/研发中心"));

    mockMvc
        .perform(get("/api/organizations/" + be))
        .andExpect(jsonPath("$.wholeName").value("总公司/研发中心/后端组"));
    mockMvc
        .perform(get("/api/organizations/" + hq))
        .andExpect(jsonPath("$.wholeName").value("总公司"));
  }

  // ---------------------------- PUT /{id}/parent move + cycle -----------

  @Test
  void putParent_moveSubtree_cascadesPathAndWholeName() throws Exception {
    Long hq1 = createRoot("HQ1", "公司一");
    Long hq2 = createRoot("HQ2", "公司二");
    Long rd = createChild(hq1, "RD", "研发部", "DEPARTMENT");
    Long be = createChild(rd, "BE", "后端组", "TEAM");

    ObjectNode body = json.createObjectNode();
    body.put("parentId", hq2);
    mockMvc
        .perform(
            put("/api/organizations/" + rd + "/parent")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.parentId").value(hq2))
        .andExpect(jsonPath("$.wholeName").value("公司二/研发部"));

    mockMvc
        .perform(get("/api/organizations/" + be))
        .andExpect(jsonPath("$.wholeName").value("公司二/研发部/后端组"));
  }

  @Test
  void putParent_moveToDescendant_returns409() throws Exception {
    Long hq = createRoot("HQ", "总公司");
    Long rd = createChild(hq, "RD", "研发部", "DEPARTMENT");
    Long be = createChild(rd, "BE", "后端组", "TEAM");

    ObjectNode body = json.createObjectNode();
    body.put("parentId", be);
    mockMvc
        .perform(
            put("/api/organizations/" + hq + "/parent")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body.toString()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("cannot move to descendant"));
  }

  // ---------------------------- DELETE -----------------------------------

  @Test
  void delete_leafNode_returns204AndSubsequentGetReturns404() throws Exception {
    Long hq = createRoot("HQ", "总公司");
    mockMvc.perform(delete("/api/organizations/" + hq)).andExpect(status().isNoContent());
    mockMvc.perform(get("/api/organizations/" + hq)).andExpect(status().isNotFound());
  }

  @Test
  void delete_nodeWithChildren_returns409() throws Exception {
    Long hq = createRoot("HQ", "总公司");
    createChild(hq, "RD", "研发部", "DEPARTMENT");
    mockMvc
        .perform(delete("/api/organizations/" + hq))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("has child organizations"));
  }

  // ---------------------------- TC-MIG-003: path /digits/digits/digits ---

  @Test
  void get_threeLevelTree_pathSegmentsAreAllNumeric() throws Exception {
    Long a = createRoot("A", "A");
    Long b = createChild(a, "B", "B", "DEPARTMENT");
    Long c = createChild(b, "C", "C", "TEAM");
    mockMvc
        .perform(get("/api/organizations/" + c))
        .andExpect(jsonPath("$.path").value("/" + a + "/" + b + "/" + c))
        .andExpect(jsonPath("$.path", matchesPattern("^/\\d+/\\d+/\\d+$")));
  }

  // ---------------------------- helpers ----------------------------------

  private Long createRoot(String code, String name) throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("type", "COMPANY");
    body.put("code", code);
    body.put("name", name);
    MvcResult r =
        mockMvc
            .perform(
                post("/api/organizations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body.toString()))
            .andExpect(status().isCreated())
            .andReturn();
    return json.readTree(r.getResponse().getContentAsString()).get("id").asLong();
  }

  private Long createChild(Long parentId, String code, String name, String type) throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("parentId", parentId);
    body.put("type", type);
    body.put("code", code);
    body.put("name", name);
    MvcResult r =
        mockMvc
            .perform(
                post("/api/organizations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body.toString()))
            .andExpect(status().isCreated())
            .andReturn();
    return json.readTree(r.getResponse().getContentAsString()).get("id").asLong();
  }
}
