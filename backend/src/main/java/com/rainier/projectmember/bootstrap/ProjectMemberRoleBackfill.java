/* (C) 2026 Rainier — internal use only. */
package com.rainier.projectmember.bootstrap;

import com.rainier.projectmember.domain.ProjectMember;
import com.rainier.projectmember.domain.ProjectMemberRoleAssignment;
import com.rainier.projectmember.repository.ProjectMemberRepository;
import com.rainier.projectmember.repository.ProjectMemberRoleAssignmentRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * v0.0.88 (C8) — 启动时把存量 {@code ProjectMember.role} 复刻一条到
 * {@code rainier_project_member_role}，保证 read 路径的 {@code roles[]} 富化非空。
 *
 * <p>Idempotent：每个 (memberId, role) 已存在则跳过；二次启动 0 写入。
 *
 * <p>Flag: {@code app.migration.project-member-role.enabled}，默认 true；测试 profile 关闭，避免
 * 干扰 PM 控制器测试的清表初始化。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
@ConditionalOnProperty(
    name = "app.migration.project-member-role.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class ProjectMemberRoleBackfill implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(ProjectMemberRoleBackfill.class);

  private final ProjectMemberRepository pmRepo;
  private final ProjectMemberRoleAssignmentRepository roleRepo;

  public ProjectMemberRoleBackfill(
      ProjectMemberRepository pmRepo, ProjectMemberRoleAssignmentRepository roleRepo) {
    this.pmRepo = pmRepo;
    this.roleRepo = roleRepo;
  }

  @Override
  @Transactional
  public void run(String... args) {
    List<ProjectMember> all = pmRepo.findAll();
    int created = 0;
    for (ProjectMember m : all) {
      String role = m.getRole();
      if (role == null || role.isEmpty()) continue;
      if (roleRepo.existsByProjectMemberIdAndProjectRole(m.getId(), role)) continue;
      ProjectMemberRoleAssignment a = new ProjectMemberRoleAssignment();
      a.setProjectMemberId(m.getId());
      a.setProjectRole(role);
      roleRepo.saveAndFlush(a);
      created++;
    }
    if (created > 0) {
      log.info(
          "ProjectMemberRoleBackfill: synced {} legacy ProjectMember.role into assignments",
          created);
    }
  }
}
