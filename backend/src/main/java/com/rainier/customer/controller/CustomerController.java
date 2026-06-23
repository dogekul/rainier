/* (C) 2026 Rainier — internal use only. */
package com.rainier.customer.controller;

import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.customer.dto.CustomerCreateRequest;
import com.rainier.customer.dto.CustomerDetail;
import com.rainier.customer.dto.CustomerUpdateRequest;
import com.rainier.customer.service.CustomerService;
import java.net.URI;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 客户 endpoints (v0.0.45). all-users (token-gated). */
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

  private final CustomerService service;

  public CustomerController(CustomerService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<CustomerDetail> create(@Valid @RequestBody CustomerCreateRequest req) {
    CustomerDetail created = service.create(req);
    return ResponseEntity.created(URI.create("/api/customers/" + created.getId())).body(created);
  }

  @GetMapping("/{id}")
  public CustomerDetail get(@PathVariable Long id) {
    return service.findById(id);
  }

  @GetMapping
  public PageResponse<CustomerDetail> list(@Valid PageParams page) {
    return service.list(page);
  }

  @PutMapping("/{id}")
  public CustomerDetail update(@PathVariable Long id, @Valid @RequestBody CustomerUpdateRequest req) {
    return service.update(id, req);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
