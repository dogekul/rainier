/* (C) 2026 Rainier — internal use only. */
package com.rainier.customer.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.customer.domain.Customer;
import com.rainier.customer.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** v0.0.45 客户 CRUD. Covers TC-CUS-001..006. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CustomerControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private CustomerRepository repo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    repo.deleteAll();
  }

  private Long seed(String name) {
    Customer c = new Customer();
    c.setName(name);
    return repo.saveAndFlush(c).getId();
  }

  /** TC-CUS-001: create → 201 with name/industry. */
  @Test
  void create_returns201() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("name", "招商银行");
    body.put("industry", "金融");
    mockMvc
        .perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("招商银行"))
        .andExpect(jsonPath("$.industry").value("金融"));
  }

  /** TC-CUS-002: blank name → 400. */
  @Test
  void create_blankName_returns400() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("name", "  ");
    mockMvc
        .perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isBadRequest());
  }

  /** TC-CUS-003: list + search by name. */
  @Test
  void list_searchByName() throws Exception {
    seed("中信集团");
    seed("招商银行");
    mockMvc
        .perform(get("/api/customers?search=招商"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.content[0].name").value("招商银行"));
  }

  /** TC-CUS-004: get by id. */
  @Test
  void get_returnsCustomer() throws Exception {
    Long id = seed("中信集团");
    mockMvc
        .perform(get("/api/customers/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("中信集团"));
  }

  /** TC-CUS-005: update name/industry. */
  @Test
  void update_changesFields() throws Exception {
    Long id = seed("旧名");
    ObjectNode body = json.createObjectNode();
    body.put("name", "新名");
    body.put("industry", "制造");
    mockMvc
        .perform(
            put("/api/customers/" + id).contentType(MediaType.APPLICATION_JSON).content(body.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("新名"))
        .andExpect(jsonPath("$.industry").value("制造"));
  }

  /** TC-CUS-006: delete → 204, then get → 404 (soft-deleted, hidden). */
  @Test
  void delete_thenGet404() throws Exception {
    Long id = seed("待删");
    mockMvc.perform(delete("/api/customers/" + id)).andExpect(status().isNoContent());
    mockMvc.perform(get("/api/customers/" + id)).andExpect(status().isNotFound());
  }
}
