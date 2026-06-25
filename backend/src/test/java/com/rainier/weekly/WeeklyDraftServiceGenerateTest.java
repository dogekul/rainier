/* (C) 2026 Rainier — internal use only. */
package com.rainier.weekly;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rainier.common.exception.ConflictException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.story.domain.Story;
import com.rainier.story.domain.StoryStatus;
import com.rainier.story.repository.StoryRepository;
import com.rainier.task.domain.Task;
import com.rainier.task.domain.TaskStatus;
import com.rainier.task.repository.TaskRepository;
import com.rainier.weekly.domain.WeeklyDraft;
import com.rainier.weekly.domain.WeeklyDraftStatus;
import com.rainier.weekly.repository.WeeklyDraftRepository;
import com.rainier.weekly.service.WeeklyDraftService;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * v0.0.71 — verifies the weekly draft template-rules generator + accept transition. Uses
 * @SpringBootTest so JPA + Specifications + Hibernate auditing run for real on H2.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WeeklyDraftServiceGenerateTest {

  @Autowired private WeeklyDraftService service;
  @Autowired private WeeklyDraftRepository draftRepo;
  @Autowired private TaskRepository taskRepo;
  @Autowired private StoryRepository storyRepo;

  private static final Long USER_ID = 9001L;
  private static final Long OTHER_USER_ID = 9002L;
  private static final Long PROJECT_ID = 7001L;
  private static final Long SPRINT_ID = 7002L;
  private final LocalDate periodStart = LocalDate.now().minusDays(6);
  private final LocalDate periodEnd = LocalDate.now();

  @BeforeEach
  void cleanDb() {
    draftRepo.deleteAll();
  }

  /** TC-WD-S-001: DONE task in window → row appears under「本周完成」段 with code+title. */
  @Test
  void generate_doneTaskInWindow_appearsInMarkdown() {
    seedTask("T-1", "修登录", TaskStatus.DONE, USER_ID);
    seedStory("S-1", "登录流程梳理", StoryStatus.DONE, USER_ID);

    WeeklyDraft draft = service.generate(USER_ID, periodStart, periodEnd);

    assertThat(draft.getId()).isNotNull();
    assertThat(draft.getStatus()).isEqualTo(WeeklyDraftStatus.DRAFT);
    assertThat(draft.getUserId()).isEqualTo(USER_ID);
    assertThat(draft.getCreatedAt()).isNotNull();
    assertThat(draft.getAcceptedAt()).isNull();
    assertThat(draft.getContentMarkdown())
        .contains("# 本周完成")
        .contains("# 进行中")
        .contains("[T-1] 修登录")
        .contains("[S-1] 登录流程梳理");
  }

  /** TC-WD-S-002: no data → still creates a DRAFT, each section reads "- 无". */
  @Test
  void generate_noData_stillCreatesDraftWithPlaceholders() {
    WeeklyDraft draft = service.generate(USER_ID, periodStart, periodEnd);

    assertThat(draft.getId()).isNotNull();
    assertThat(draft.getStatus()).isEqualTo(WeeklyDraftStatus.DRAFT);
    assertThat(draft.getContentMarkdown())
        .contains("# 本周完成")
        .contains("# 进行中")
        .contains("- 无");
  }

  /** TC-WD-S-003: other user's DONE task is NOT picked up. */
  @Test
  void generate_otherUserTask_isFilteredOut() {
    seedTask("T-OTHER", "他人任务", TaskStatus.DONE, OTHER_USER_ID);

    WeeklyDraft draft = service.generate(USER_ID, periodStart, periodEnd);

    assertThat(draft.getContentMarkdown()).doesNotContain("他人任务");
  }

  /** TC-WD-S-004: IN_PROGRESS task lands in 进行中 section regardless of update window. */
  @Test
  void generate_inProgressTask_appearsInOngoing() {
    seedTask("T-2", "正在做", TaskStatus.IN_PROGRESS, USER_ID);

    WeeklyDraft draft = service.generate(USER_ID, periodStart, periodEnd);

    String md = draft.getContentMarkdown();
    int doneIdx = md.indexOf("# 本周完成");
    int ongoingIdx = md.indexOf("# 进行中");
    int inProgressLineIdx = md.indexOf("[T-2] 正在做");
    assertThat(doneIdx).isGreaterThanOrEqualTo(0);
    assertThat(ongoingIdx).isGreaterThan(doneIdx);
    assertThat(inProgressLineIdx).isGreaterThan(ongoingIdx);
  }

  /** TC-WD-A-001: accept flips DRAFT → ACCEPTED and sets acceptedAt. */
  @Test
  void accept_draft_transitionsToAccepted() {
    WeeklyDraft draft = service.generate(USER_ID, periodStart, periodEnd);

    WeeklyDraft accepted = service.accept(draft.getId());

    assertThat(accepted.getStatus()).isEqualTo(WeeklyDraftStatus.ACCEPTED);
    assertThat(accepted.getAcceptedAt()).isNotNull();
  }

  /** TC-WD-A-002: second accept on the same draft → 409 ConflictException. */
  @Test
  void accept_alreadyAccepted_throwsConflict() {
    WeeklyDraft draft = service.generate(USER_ID, periodStart, periodEnd);
    service.accept(draft.getId());

    assertThatThrownBy(() -> service.accept(draft.getId())).isInstanceOf(ConflictException.class);
  }

  /** TC-WD-A-003: accept unknown id → NotFoundException. */
  @Test
  void accept_missingId_throwsNotFound() {
    assertThatThrownBy(() -> service.accept(999_999L)).isInstanceOf(NotFoundException.class);
  }

  // ---- seed helpers ----

  private void seedTask(String code, String title, String status, Long assigneeUserId) {
    Task t = new Task();
    t.setCode(code);
    t.setTitle(title);
    t.setStatus(status);
    t.setPriority("MEDIUM");
    t.setProjectId(PROJECT_ID);
    t.setAssigneeUserId(assigneeUserId);
    taskRepo.saveAndFlush(t);
  }

  private void seedStory(String code, String title, String status, Long ownerUserId) {
    Story s = new Story();
    s.setCode(code);
    s.setTitle(title);
    s.setStatus(status);
    s.setPriority("MEDIUM");
    s.setSprintId(SPRINT_ID);
    s.setProjectId(PROJECT_ID);
    s.setOwnerUserId(ownerUserId);
    storyRepo.saveAndFlush(s);
  }
}
