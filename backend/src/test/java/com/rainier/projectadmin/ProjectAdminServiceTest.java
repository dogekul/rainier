/* (C) 2026 Rainier — internal use only. */
package com.rainier.projectadmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rainier.common.exception.NotFoundException;
import com.rainier.project.domain.Project;
import com.rainier.project.domain.ProjectStatus;
import com.rainier.project.domain.ProjectType;
import com.rainier.project.repository.ProjectRepository;
import com.rainier.projectadmin.service.ProjectAdminService;
import com.rainier.projectmember.domain.ProjectMember;
import com.rainier.projectmember.domain.ProjectMemberRole;
import com.rainier.projectmember.repository.ProjectMemberRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** v0.0.78 (B5) — ProjectAdminService unit-ish tests via real JPA. */
@SpringBootTest
@ActiveProfiles("test")
class ProjectAdminServiceTest {

  @Autowired private ProjectAdminService service;
  @Autowired private ProjectMemberRepository memberRepo;
  @Autowired private ProjectRepository projectRepo;
  @Autowired private UserRepository userRepo;

  private Long projectId;
  private Long projectId2;
  private Long aliceId;
  private Long bobId;

  @BeforeEach
  void setUp() {
    memberRepo.deleteAll();
    projectRepo.deleteAll();
    userRepo.deleteAll();

    aliceId = newUser("alice-padmin", "Alice");
    bobId = newUser("bob-padmin", "Bob");
    projectId = newProject("PA-1", aliceId);
    projectId2 = newProject("PA-2", aliceId);
  }

  private Long newUser(String login, String name) {
    User u = new User();
    u.setLoginName(login);
    u.setName(name);
    u.setEnabled(Boolean.TRUE);
    return userRepo.saveAndFlush(u).getId();
  }

  private Long newProject(String code, Long ownerId) {
    Project p = new Project();
    p.setCode(code);
    p.setName(code);
    p.setStatus(ProjectStatus.ACTIVE);
    p.setProjectType(ProjectType.EXTERNAL_DELIVERY);
    p.setOwnerUserId(ownerId);
    p.setEnabled(Boolean.TRUE);
    return projectRepo.saveAndFlush(p).getId();
  }

  /** TC-PADMIN-001: grant when user has no ProjectMember row → creates one with role=OTHER, flag=true. */
  @Test
  void grant_creates_new_member_row() {
    List<Long> admins = service.updateGrant(projectId, bobId, "alice-padmin");
    assertThat(admins).containsExactly(bobId);
    ProjectMember m = memberRepo.findByProjectIdAndUserId(projectId, bobId).get();
    assertThat(m.getRole()).isEqualTo(ProjectMemberRole.OTHER);
    assertThat(m.getIsProjectAdmin()).isTrue();
    assertThat(m.getJoinedBy()).isEqualTo("alice-padmin");
  }

  /** TC-PADMIN-002: grant when user is already a member → just flips flag, role preserved. */
  @Test
  void grant_preserves_existing_role() {
    ProjectMember m = new ProjectMember();
    m.setProjectId(projectId);
    m.setUserId(bobId);
    m.setRole(ProjectMemberRole.DEV);
    m.setJoinedAt(Instant.now());
    m.setJoinedBy("alice-padmin");
    memberRepo.saveAndFlush(m);

    service.updateGrant(projectId, bobId, "alice-padmin");
    ProjectMember after = memberRepo.findByProjectIdAndUserId(projectId, bobId).get();
    assertThat(after.getRole()).isEqualTo(ProjectMemberRole.DEV);
    assertThat(after.getIsProjectAdmin()).isTrue();
  }

  /** TC-PADMIN-003: revoke flips flag false but keeps row. */
  @Test
  void revoke_keeps_member_row() {
    service.updateGrant(projectId, bobId, "alice-padmin");
    service.updateRevoke(projectId, bobId);
    ProjectMember after = memberRepo.findByProjectIdAndUserId(projectId, bobId).get();
    assertThat(after.getIsProjectAdmin()).isFalse();
    assertThat(after.getRole()).isEqualTo(ProjectMemberRole.OTHER);
  }

  /** TC-PADMIN-003b: revoke on non-member is idempotent (no throw, returns empty admin list). */
  @Test
  void revoke_idempotent_on_missing_row() {
    List<Long> admins = service.updateRevoke(projectId, bobId);
    assertThat(admins).isEmpty();
  }

  /** TC-PADMIN-004: isProjectAdmin true / false / missing. */
  @Test
  void is_project_admin_truth_table() {
    assertThat(service.isProjectAdmin(bobId, projectId)).isFalse();
    service.updateGrant(projectId, bobId, "alice-padmin");
    assertThat(service.isProjectAdmin(bobId, projectId)).isTrue();
    assertThat(service.isProjectAdmin(bobId, projectId2)).isFalse();
    assertThat(service.isProjectAdmin(null, projectId)).isFalse();
    assertThat(service.isProjectAdmin(bobId, null)).isFalse();
  }

  /** TC-PADMIN-005: listProjectAdmins / listAdminProjects. */
  @Test
  void list_endpoints() {
    service.updateGrant(projectId, bobId, "alice-padmin");
    service.updateGrant(projectId2, bobId, "alice-padmin");
    assertThat(service.listProjectAdmins(projectId)).containsExactly(bobId);
    assertThat(service.listAdminProjects(bobId)).containsExactlyInAnyOrder(projectId, projectId2);
    assertThat(service.listAdminProjects(aliceId)).isEmpty();
  }

  /** Unknown project → 404 on grant. */
  @Test
  void grant_unknown_project_404() {
    assertThatThrownBy(() -> service.updateGrant(99999L, bobId, "alice-padmin"))
        .isInstanceOf(NotFoundException.class);
  }

  /** Unknown user → 404 on grant. */
  @Test
  void grant_unknown_user_404() {
    assertThatThrownBy(() -> service.updateGrant(projectId, 99999L, "alice-padmin"))
        .isInstanceOf(NotFoundException.class);
  }
}
