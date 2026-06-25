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
}
