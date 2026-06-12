/* (C) 2026 Rainier — internal use only. */
package com.rainier.story.controller;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rainier.common.domain.Priority;
import com.rainier.story.domain.Story;
import com.rainier.story.domain.StoryStatus;
import com.rainier.story.repository.StoryRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** TC-STORY-OWN-001 — GET /api/stories?ownerUserId= filter (v0.0.18 workbench 我的 Story). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StoryOwnerFilterTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private StoryRepository repo;
  @Autowired private UserRepository userRepo;

  @BeforeEach
  void cleanDb() {
    repo.deleteAll();
    userRepo.deleteAll();
  }

  private Long seedUser(String loginName) {
    User u = new User();
    u.setLoginName(loginName);
    u.setName(loginName);
    u.setIsInternal(true);
    u.setEnabled(true);
    return userRepo.saveAndFlush(u).getId();
  }

  private void seedStory(String code, Long ownerId) {
    Story s = new Story();
    s.setCode(code);
    s.setTitle("T-" + code);
    s.setSprintId(1L); // enrichment is null-tolerant; the filter only cares about ownerUserId
    s.setProjectId(1L);
    s.setOwnerUserId(ownerId);
    s.setStatus(StoryStatus.DRAFT);
    s.setPriority(Priority.MEDIUM);
    repo.saveAndFlush(s);
  }

  /** TC-STORY-OWN-001: only the owner's stories are returned. */
  @Test
  void getList_filterByOwnerUserId() throws Exception {
    Long u1 = seedUser("alice");
    Long u2 = seedUser("bob");
    seedStory("S-1", u1);
    seedStory("S-2", u1);
    seedStory("S-3", u2);

    mockMvc
        .perform(get("/api/stories?ownerUserId=" + u1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.content[*].ownerUserId", everyItem(is(u1.intValue()))));

    // Omitting the param keeps the filter optional — all three stories returned.
    mockMvc
        .perform(get("/api/stories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(3));
  }
}
