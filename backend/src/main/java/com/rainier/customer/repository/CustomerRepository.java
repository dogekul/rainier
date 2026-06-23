/* (C) 2026 Rainier — internal use only. */
package com.rainier.customer.repository;

import com.rainier.customer.domain.Customer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository
    extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {

  /** For「type a new name」on opportunity create — reuse an existing customer with the same name. */
  Optional<Customer> findFirstByNameIgnoreCase(String name);
}
