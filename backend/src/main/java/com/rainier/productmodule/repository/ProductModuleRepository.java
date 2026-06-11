/* (C) 2026 Rainier — internal use only. */
package com.rainier.productmodule.repository;

import com.rainier.productmodule.domain.ProductModule;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductModuleRepository
    extends JpaRepository<ProductModule, Long>, JpaSpecificationExecutor<ProductModule> {

  long countByProductId(Long productId);

  long countByParentId(Long parentId);

  List<ProductModule> findByParentId(Long parentId);

  /** Service-layer guard for top-level scope — the DB composite UQ skips NULL parent_id rows. */
  boolean existsByProductIdAndParentIdIsNullAndCode(Long productId, String code);

  boolean existsByParentIdAndCode(Long parentId, String code);
}
