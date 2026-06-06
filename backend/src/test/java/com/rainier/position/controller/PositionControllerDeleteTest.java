/* (C) 2026 Rainier — internal use only. */
package com.rainier.position.controller;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rainier.position.repository.PositionRepository;
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

/** Integration tests for {@link PositionController} DELETE. Covers TC-POS-009..010. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PositionControllerDeleteTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private PositionRepository repo;
  @Autowired private UserRepository userRepo;
  @Autowired private ObjectMapper json;

  @BeforeEach
  void cleanDb() {
    userRepo.deleteAll();
    repo.deleteAll();
  }

  private Long createPosition() throws Exception {
    ObjectNode body = json.createObjectNode();
    body.put("code", "BE_ENG");
    body.put("name", "Backend Engineer");
    body.put("category", "TECH");
    MvcResult res =
        mockMvc
            .perform(
                post("/api/positions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body.toString()))
            .andExpect(status().isCreated())
            .andReturn();
    return json.readTree(res.getResponse().getContentAsString()).get("id").asLong();
  }

  /** TC-POS-009: 无 User 引用软删成功。 */
  @Test
  void delete_noUserRef_returns204AndGet404() throws Exception {
    Long id = createPosition();
    mockMvc.perform(delete("/api/positions/" + id)).andExpect(status().isNoContent());
    mockMvc.perform(get("/api/positions/" + id)).andExpect(status().isNotFound());
  }

  /** TC-POS-010: 有 User 引用被拒。 */
  @Test
  void delete_withUserRef_returns409() throws Exception {
    Long positionId = createPosition();
    User u = new User();
    u.setLoginName("alice");
    u.setName("Alice");
    u.setIsInternal(true);
    u.setEnabled(true);
    u.setPositionId(positionId);
    userRepo.saveAndFlush(u);

    mockMvc
        .perform(delete("/api/positions/" + positionId))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message", startsWith("position has assigned users")));
  }
}
