/* (C) 2026 Rainier — internal use only. */
package com.rainier.projectmember.repository;

import com.rainier.projectmember.domain.ProjectMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjectMemberRepository
    extends JpaRepository<ProjectMember, Long>, JpaSpecificationExecutor<ProjectMember> {

  List<ProjectMember> findByProjectIdOrderByJoinedAtDesc(Long projectId);

  Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

  boolean existsByProjectIdAndUserId(Long projectId, Long userId);

  List<ProjectMember> findByUserId(Long userId);

  /** v0.0.78 (B5) — 项目的全部"项目管理员"行。 */
  List<ProjectMember> findByProjectIdAndIsProjectAdminTrue(Long projectId);

  /** v0.0.78 (B5) — 该用户作为"项目管理员"参与的全部项目。 */
  List<ProjectMember> findByUserIdAndIsProjectAdminTrue(Long userId);
}
