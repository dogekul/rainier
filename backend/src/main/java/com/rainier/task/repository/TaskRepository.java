/* (C) 2026 Rainier — internal use only. */
package com.rainier.task.repository;

import com.rainier.task.domain.Task;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/** Repository for {@link Task}. */
@Repository
public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

  boolean existsByCode(String code);

  /** Used by ProjectService.delete for FK chain — only counts del_flag=0 rows (via @Where). */
  long countByProjectId(Long projectId);

  /** v0.0.28 portfolio fan-out — active tasks across a set of projects. */
  List<Task> findByProjectIdIn(Collection<Long> projectIds);
}
