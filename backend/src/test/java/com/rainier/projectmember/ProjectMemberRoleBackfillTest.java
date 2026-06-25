/* (C) 2026 Rainier — internal use only. */
package com.rainier.projectmember;

import com.rainier.projectmember.bootstrap.ProjectMemberRoleBackfill;
import com.rainier.projectmember.domain.ProjectMember;
import com.rainier.projectmember.repository.ProjectMemberRepository;
import com.rainier.projectmember.repository.ProjectMemberRoleAssignmentRepository;
import java.time.Instant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * v0.0.88 (C8) — 存量 ProjectMember.role 启动后被 backfill 写入 assignment 表，二次启动 idempotent.
 *
 * <p>用 {@link TestPropertySource} 在本测试里 flip {@code app.migration.project-member-role.enabled=true}.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {"app.migration.project-member-role.enabled=true"})
class ProjectMemberRoleBackfillTest {

  @Autowired private ProjectMemberRepository pmRepo;
  @Autowired private ProjectMemberRoleAssignmentRepository roleRepo;
  @Autowired private ProjectMemberRoleBackfill backfill;

  @BeforeEach
  void cleanDb() {
    roleRepo.deleteAll();
    pmRepo.deleteAll();
  }

  @Test
  void backfill_syncs_legacy_role_then_idempotent() {
    // 1. seed: two ProjectMember 直接 insert，模拟存量（不经 service，所以 assignment 表是空的）
    ProjectMember m1 = new ProjectMember();
    m1.setProjectId(1001L);
    m1.setUserId(2001L);
    m1.setRole("DEV");
    m1.setJoinedAt(Instant.now());
    m1.setJoinedBy("system");
    pmRepo.saveAndFlush(m1);

    ProjectMember m2 = new ProjectMember();
    m2.setProjectId(1001L);
    m2.setUserId(2002L);
    m2.setRole("QA");
    m2.setJoinedAt(Instant.now());
    m2.setJoinedBy("system");
    pmRepo.saveAndFlush(m2);

    Assertions.assertEquals(0, roleRepo.count());

    // 2. 手动触发 backfill（模拟 startup runner）
    backfill.run();
    Assertions.assertEquals(2, roleRepo.count());

    // 3. 二次运行 idempotent
    backfill.run();
    Assertions.assertEquals(2, roleRepo.count());
  }
}
