/* (C) 2026 Rainier — internal use only. */
package com.rainier.operationissue.repository;

import com.rainier.operationissue.domain.OperationIssue;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** v0.0.58 — 运营问题清单仓库。@Where(del_flag=0) 由 entity 提供。 */
@Repository
public interface OperationIssueRepository extends JpaRepository<OperationIssue, Long> {
  List<OperationIssue> findByOperationIdOrderByIdDesc(Long operationId);
}
