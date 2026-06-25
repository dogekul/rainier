/* (C) 2026 Rainier — internal use only. */
package com.rainier.projectmember.repository;

import com.rainier.projectmember.domain.ProjectMemberRoleAssignment;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** v0.0.88 (C8) — ProjectMember ↔ project role 多对多关联表 repository. */
@Repository
public interface ProjectMemberRoleAssignmentRepository
    extends JpaRepository<ProjectMemberRoleAssignment, Long> {

  List<ProjectMemberRoleAssignment> findByProjectMemberId(Long projectMemberId);

  List<ProjectMemberRoleAssignment> findByProjectMemberIdIn(
      Collection<Long> projectMemberIds);

  Optional<ProjectMemberRoleAssignment> findByProjectMemberIdAndProjectRole(
      Long projectMemberId, String projectRole);

  boolean existsByProjectMemberIdAndProjectRole(Long projectMemberId, String projectRole);
}
