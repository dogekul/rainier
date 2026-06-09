/* (C) 2026 Rainier — internal use only. */
package com.rainier.product.repository;

import com.rainier.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository
    extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

  boolean existsByCode(String code);

  long countByCategoryId(Long categoryId);
}
