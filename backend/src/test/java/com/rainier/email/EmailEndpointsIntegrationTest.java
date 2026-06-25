/* (C) 2026 Rainier — internal use only. */
package com.rainier.email;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rainier.auth.controller.AuthController;
import com.rainier.notification.domain.Notification;
import com.rainier.notification.repository.NotificationRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * v0.0.92 (D4) — 端点集成测试：GET /api/emails、POST /api/me/notifications/{id}/email。 admin-authz
 * 在 test profile 默认关闭，所以 GET /api/emails 可直接通过 token attr。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmailEndpointsIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private SentEmailRecordRepository sentRepo;
  @Autowired private NotificationRepository notifRepo;
  @Autowired private UserRepository userRepo;

  private Long aliceId;
  private Long bobId;

  @BeforeEach
  void clean() {
    sentRepo.deleteAll();
    notifRepo.deleteAll();
    userRepo.deleteAll();

    User alice = new User();
    alice.setLoginName("alice_d4");
    alice.setName("Alice");
    alice.setEmailAddress("alice@x.com");
    alice.setEnabled(Boolean.TRUE);
    aliceId = userRepo.saveAndFlush(alice).getId();

    User bob = new User();
    bob.setLoginName("bob_d4");
    bob.setName("Bob");
    bob.setEmailAddress(null);
    bob.setEnabled(Boolean.TRUE);
    bobId = userRepo.saveAndFlush(bob).getId();
  }

  /** GET /api/emails 分页返回（admin-authz 在 test profile 关闭）。 */
  @Test
  void listSentEmails_returnsPage() throws Exception {
    // 先插一行
    SentEmailRecord r = new SentEmailRecord();
    r.setFromAddr("noreply@rainier.local");
    r.setToAddrsJson("[\"a@x.com\"]");
    r.setSubject("hi");
    r.setBodyTextSnippet("hello");
    r.setSentAt(LocalDateTime.now());
    r.setStatus(SentEmailRecord.STATUS_SENT);
    sentRepo.saveAndFlush(r);

    mockMvc
        .perform(get("/api/emails?page=0&size=10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.content[0].subject").value("hi"))
        .andExpect(jsonPath("$.content[0].status").value("SENT"));
  }

  /** POST /api/me/notifications/{id}/email 成功 → 200 + 落库一条 SentEmailRecord。 */
  @Test
  void forwardNotificationToEmail_persistsSentRecord() throws Exception {
    Notification n = new Notification();
    n.setUserId(aliceId);
    n.setTitle("项目风险");
    n.setBody("详情请见看板");
    n.setLevel("CRIT");
    n.setCreatedAt(LocalDateTime.now());
    n = notifRepo.saveAndFlush(n);

    mockMvc
        .perform(
            post("/api/me/notifications/" + n.getId() + "/email")
                .requestAttr(AuthController.ATTR_USERNAME, "alice_d4"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));

    // 同时验证 SentEmailRecord 已落库
    org.assertj.core.api.Assertions.assertThat(sentRepo.count()).isEqualTo(1L);
    SentEmailRecord row = sentRepo.findAll().get(0);
    org.assertj.core.api.Assertions.assertThat(row.getToAddrsJson()).contains("alice@x.com");
    org.assertj.core.api.Assertions.assertThat(row.getSubject()).contains("项目风险");
  }

  /** 当前用户无 emailAddress → 400. */
  @Test
  void forwardNotification_userNoEmail_400() throws Exception {
    Notification n = new Notification();
    n.setUserId(bobId);
    n.setTitle("t");
    n.setLevel("INFO");
    n.setCreatedAt(LocalDateTime.now());
    n = notifRepo.saveAndFlush(n);

    mockMvc
        .perform(
            post("/api/me/notifications/" + n.getId() + "/email")
                .requestAttr(AuthController.ATTR_USERNAME, "bob_d4"))
        .andExpect(status().isBadRequest());
  }

  /** 转发别人的通知 → 404. */
  @Test
  void forwardNotification_notOwner_404() throws Exception {
    Notification n = new Notification();
    n.setUserId(bobId);
    n.setTitle("t");
    n.setLevel("INFO");
    n.setCreatedAt(LocalDateTime.now());
    n = notifRepo.saveAndFlush(n);

    mockMvc
        .perform(
            post("/api/me/notifications/" + n.getId() + "/email")
                .requestAttr(AuthController.ATTR_USERNAME, "alice_d4"))
        .andExpect(status().isNotFound());
  }
}
