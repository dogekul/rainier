/* (C) 2026 Rainier — internal use only. */
package com.rainier.position.repository;

import com.rainier.position.domain.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/** Repository for {@link Position}. */
@Repository
public interface PositionRepository
    extends JpaRepository<Position, Long>, JpaSpecificationExecutor<Position> {

  boolean existsByCode(String code);
}
