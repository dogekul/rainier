/* (C) 2026 Rainier — internal use only. */
package com.rainier.productcategory.repository;

import com.rainier.productcategory.domain.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductCategoryRepository
    extends JpaRepository<ProductCategory, Long>, JpaSpecificationExecutor<ProductCategory> {

  boolean existsByCode(String code);
}
