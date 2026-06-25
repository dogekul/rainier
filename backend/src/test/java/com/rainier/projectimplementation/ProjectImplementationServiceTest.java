/* (C) 2026 Rainier — internal use only. */
package com.rainier.projectimplementation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.project.domain.Project;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.projectimplementation.dto.ProjectImplementationDetail;
import com.rainier.projectimplementation.dto.ProjectImplementationUpsertRequest;
import com.rainier.projectimplementation.repository.ProjectImplementationRepository;
import com.rainier.projectimplementation.service.ProjectImplementationService;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** v0.0.89 — D1 project-implementation-form. Covers PIF-001/002/005. */
@SpringBootTest
@ActiveProfiles("test")
class ProjectImplementationServiceTest {

  @Autowired private ProjectImplementationService service;
  @Autowired private ProjectImplementationRepository repo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private UserRepository userRepo;

  @BeforeEach
  void cleanDb() {
    repo.deleteAll();
  }

  private Long seedProject(String code) {
    User u = new User();
    u.setLoginName("pif-owner-" + code);
    u.setName("pif-owner-" + code);
    u.setIsInternal(true);
    u.setEnabled(true);
    Long uid = userRepo.saveAndFlush(u).getId();

    Project p = new Project();
    p.setCode(code);
    p.setName(code);
    p.setStatus("ACTIVE");
    p.setOwnerUserId(uid);
    p.setEnabled(true);
    p.setProjectType("CONTRACT");
    return projectRepo.saveAndFlush(p).getId();
  }

  /** PIF-001/002: createOrUpdate is idempotent — same projectId returns same id. */
  @Test
  void createOrUpdate_idempotent() {
    Long projectId = seedProject("PIF-001");

    ProjectImplementationUpsertRequest req1 = new ProjectImplementationUpsertRequest();
    req1.setScopeMarkdown("# 范围 v1\n- 模块A");
    req1.setEstimatedManDays(60);
    req1.setRiskNotes("注意上线窗口");
    ProjectImplementationDetail first = service.createOrUpdate(projectId, req1);
    assertThat(first.getId()).isNotNull();
    assertThat(first.getProjectId()).isEqualTo(projectId);
    assertThat(first.getEstimatedManDays()).isEqualTo(60);

    ProjectImplementationUpsertRequest req2 = new ProjectImplementationUpsertRequest();
    req2.setScopeMarkdown("# 范围 v2\n- 模块A\n- 模块B");
    req2.setEstimatedManDays(90);
    req2.setKeyMilestonesJson("[{\"name\":\"M1\",\"date\":\"2026-07-01\"}]");
    ProjectImplementationDetail second = service.createOrUpdate(projectId, req2);

    assertThat(second.getId()).isEqualTo(first.getId());
    assertThat(second.getScopeMarkdown()).contains("v2");
    assertThat(second.getEstimatedManDays()).isEqualTo(90);
    assertThat(second.getKeyMilestonesJson()).contains("M1");
    assertThat(repo.count()).isEqualTo(1L);
  }

  /** PIF-005: project not found → 400. */
  @Test
  void createOrUpdate_unknownProject_throws() {
    ProjectImplementationUpsertRequest req = new ProjectImplementationUpsertRequest();
    req.setScopeMarkdown("scope");
    assertThatThrownBy(() -> service.createOrUpdate(99999L, req))
        .isInstanceOf(BadRequestException.class);
  }

  /** PIF-003: findByProjectId throws NotFound when missing. */
  @Test
  void findByProjectId_missing_throws() {
    Long projectId = seedProject("PIF-NOPE");
    assertThatThrownBy(() -> service.findByProjectId(projectId))
        .isInstanceOf(NotFoundException.class);
  }
}
