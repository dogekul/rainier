/* (C) 2026 Rainier — internal use only. */
package com.rainier.organization.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
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
 * Integration tests for Organization read / update / move / delete endpoints.
 *
 * <p>Covers TC-ORG-006..016 (excluding 010 — search test stays in this file). The user_organization
 * FK protection scenario (TC-ORG-016) is currently a TODO in service code; the cleanup slice (Z01)
 * will re-enable that check once the M2M slice (B17/B18) lands.
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
    String id = createRoot("HQ", "总公司");
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

  @Test
  void get_softDeletedId_returns404() throws Exception {
    String id = createRoot("X", "X");
    mockMvc.perform(delete("/api/organizations/" + id)).andExpect(status().isNoContent());
    mockMvc.perform(get("/api/organizations/" + id)).andExpect(status().isNotFound());
  }

  // ---------------------------- GET /tree --------------------------------

  @Test
  void getTree_excludesSoftDeletedAndSortsByPath() throws Exception {
    String hq = createRoot("HQ", "总公司");
    String rd = createChild(hq, "RD", "研发部", "DEPARTMENT");
    String ops = createChild(hq, "OPS", "运维部", "DEPARTMENT");
    String backend = createChild(rd, "BE", "后端组", "TEAM");
    // Soft-delete one
    mockMvc.perform(delete("/api/organizations/" + ops)).andExpect(status().isNoContent());

    MvcResult result =
        mockMvc
            .perform(get("/api/organizations/tree"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(3))
            .andReturn();
    String body = result.getResponse().getContentAsString();
    // Verify path ordering
    assertThat(body.indexOf(hq)).as("hq before rd").isLessThan(body.indexOf(rd));
    assertThat(body.indexOf(rd)).as("rd before backend").isLessThan(body.indexOf(backend));
  }

  // ---------------------------- GET / (list) -----------------------------

  @Test
  void getList_filterByType_returnsOnlyMatchingType() throws Exception {
    String hq = createRoot("HQ", "总公司");
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
    String hq = createRoot("HQ", "总公司");
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
    String hq = createRoot("HQ", "总公司");
    String rd = createChild(hq, "RD", "研发部", "DEPARTMENT");
    String be = createChild(rd, "BE", "后端组", "TEAM");

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
    String hq1 = createRoot("HQ1", "公司一");
    String hq2 = createRoot("HQ2", "公司二");
    String rd = createChild(hq1, "RD", "研发部", "DEPARTMENT");
    String be = createChild(rd, "BE", "后端组", "TEAM");

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
    String hq = createRoot("HQ", "总公司");
    String rd = createChild(hq, "RD", "研发部", "DEPARTMENT");
    String be = createChild(rd, "BE", "后端组", "TEAM");

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
    String hq = createRoot("HQ", "总公司");
    mockMvc.perform(delete("/api/organizations/" + hq)).andExpect(status().isNoContent());
    mockMvc.perform(get("/api/organizations/" + hq)).andExpect(status().isNotFound());
  }

  @Test
  void delete_nodeWithChildren_returns409() throws Exception {
    String hq = createRoot("HQ", "总公司");
    createChild(hq, "RD", "研发部", "DEPARTMENT");
    mockMvc
        .perform(delete("/api/organizations/" + hq))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value("has child organizations"));
  }

  // ---------------------------- helpers ----------------------------------

  private String createRoot(String code, String name) throws Exception {
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
    return json.readTree(r.getResponse().getContentAsString()).get("id").asText();
  }

  private String createChild(String parentId, String code, String name, String type)
      throws Exception {
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
    return json.readTree(r.getResponse().getContentAsString()).get("id").asText();
  }
}
