/* (C) 2026 Rainier — internal use only. */
package com.rainier.event.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rainier.aiworklog.domain.AiWorkLog;
import com.rainier.aiworklog.domain.AiWorkLogStatus;
import com.rainier.aiworklog.repository.AiWorkLogRepository;
import com.rainier.event.domain.Event;
import com.rainier.event.repository.EventRepository;
import java.util.List;
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
 * v0.0.101 F2 — GitLab webhook ingress: token check, push/MR parsing, end-to-end pipeline drain.
 * Asserts the controller -> EventService.record -> process -> GitLabAdapter -> StatusSyncService
 * chain produces an AiWorkLog PROPOSED for PR_MERGE events.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GitLabWebhookControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private EventRepository eventRepo;
  @Autowired private AiWorkLogRepository aiRepo;

  @BeforeEach
  void clean() {
    aiRepo.deleteAll();
    eventRepo.deleteAll();
  }

  @Test
  void validToken_pushPayload_recordsEventAndExtractsTaskRef() throws Exception {
    String payload =
        "{\"object_kind\":\"push\",\"after\":\"abc123\","
            + "\"commits\":[{\"message\":\"fix RA-42 done\"}]}";

    MvcResult res =
        mockMvc
            .perform(
                post("/api/webhooks/gitlab")
                    .header("X-Gitlab-Token", "test-secret")
                    .header("X-Gitlab-Event", "Push Hook")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.eventId").isNumber())
            .andExpect(jsonPath("$.processed").value(1))
            .andExpect(jsonPath("$.eventKind").value("COMMIT"))
            .andReturn();
    assertThat(res.getResponse().getContentAsString()).contains("eventId");

    List<Event> all = eventRepo.findAll();
    assertThat(all).hasSize(1);
    Event saved = all.get(0);
    assertThat(saved.getSourceType()).isEqualTo("GITLAB");
    assertThat(saved.getEventKind()).isEqualTo("COMMIT");
    assertThat(saved.getSourceId()).isEqualTo("abc123");
    assertThat(saved.getProcessed()).isTrue();
    assertThat(saved.getExtractedEntityType()).isEqualTo("TASK");
    assertThat(saved.getExtractedEntityId()).isEqualTo(42L);
  }

  @Test
  void validToken_mergeRequestPayload_createsProposedAiWorkLog() throws Exception {
    String payload =
        "{\"object_kind\":\"merge_request\",\"object_attributes\":{"
            + "\"action\":\"merge\",\"iid\":7,\"title\":\"ship RA-100\"}}";

    mockMvc
        .perform(
            post("/api/webhooks/gitlab")
                .header("X-Gitlab-Token", "test-secret")
                .header("X-Gitlab-Event", "Merge Request Hook")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.eventKind").value("PR_MERGE"))
        .andExpect(jsonPath("$.processed").value(1));

    List<Event> events = eventRepo.findAll();
    assertThat(events).hasSize(1);
    Event saved = events.get(0);
    assertThat(saved.getEventKind()).isEqualTo("PR_MERGE");
    assertThat(saved.getSourceId()).isEqualTo("7");
    assertThat(saved.getExtractedEntityType()).isEqualTo("TASK");
    assertThat(saved.getExtractedEntityId()).isEqualTo(100L);

    List<AiWorkLog> logs = aiRepo.findAll();
    assertThat(logs).hasSize(1);
    AiWorkLog log = logs.get(0);
    assertThat(log.getStatus()).isEqualTo(AiWorkLogStatus.PROPOSED);
    assertThat(log.getAgentType()).isEqualTo("STATUS_SYNC");
    assertThat(log.getAction()).isEqualTo("UPDATE_TASK_STATUS");
    assertThat(log.getTargetType()).isEqualTo("TASK");
    assertThat(log.getTargetId()).isEqualTo(100L);
  }

  @Test
  void wrongToken_returns401_andPersistsNothing() throws Exception {
    String payload =
        "{\"object_kind\":\"push\",\"after\":\"abc123\","
            + "\"commits\":[{\"message\":\"RA-1 ok\"}]}";

    mockMvc
        .perform(
            post("/api/webhooks/gitlab")
                .header("X-Gitlab-Token", "wrong-secret")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.message").exists());

    assertThat(eventRepo.count()).isZero();
    assertThat(aiRepo.count()).isZero();
  }

  @Test
  void missingToken_returns401() throws Exception {
    String payload = "{\"object_kind\":\"push\",\"after\":\"x\"}";
    mockMvc
        .perform(
            post("/api/webhooks/gitlab")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isUnauthorized());
    assertThat(eventRepo.count()).isZero();
  }
}
