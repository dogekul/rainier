/* (C) 2026 Rainier — internal use only. */
package com.rainier.productmodule.repository;

import com.rainier.productmodule.domain.ProductModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductModuleRepository
    extends JpaRepository<ProductModule, Long>, JpaSpecificationExecutor<ProductModule> {

  boolean existsByCode(String code);

  long countByProductId(Long productId);
}
