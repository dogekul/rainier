/* (C) 2026 Rainier — internal use only. */
package com.rainier.demand.repository;

import com.rainier.demand.domain.Demand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/** Repository for {@link Demand}. Derived queries inherit {@code @Where("del_flag = 0")}. */
@Repository
public interface DemandRepository
    extends JpaRepository<Demand, Long>, JpaSpecificationExecutor<Demand> {

  /**
   * Hard delete bypassing {@code @SQLDelete} + {@code @Where} (used by test cleanup so soft-deleted
   * rows from previous tests don't leak into the next).
   */
  @org.springframework.data.jpa.repository.Modifying
  @org.springframework.data.jpa.repository.Query(
      value = "DELETE FROM rainier_demand",
      nativeQuery = true)
  void hardDeleteAll();
}
