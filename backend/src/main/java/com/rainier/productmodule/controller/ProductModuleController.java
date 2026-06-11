/* (C) 2026 Rainier — internal use only. */
package com.rainier.productmodule.controller;

import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.productmodule.dto.ProductModuleCreateRequest;
import com.rainier.productmodule.dto.ProductModuleDetail;
import com.rainier.productmodule.dto.ProductModuleUpdateRequest;
import com.rainier.productmodule.service.ProductModuleService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/product-modules")
public class ProductModuleController {

  private final ProductModuleService service;

  public ProductModuleController(ProductModuleService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<ProductModuleDetail> create(
      @Valid @RequestBody ProductModuleCreateRequest req) {
    ProductModuleDetail created = service.create(req);
    return ResponseEntity.created(URI.create("/api/product-modules/" + created.getId()))
        .body(created);
  }

  @GetMapping("/{id}")
  public ProductModuleDetail get(@PathVariable Long id) {
    return service.findById(id);
  }

  @GetMapping
  public PageResponse<ProductModuleDetail> list(
      @RequestParam(required = false) Long productId,
      @RequestParam(required = false) Long parentId,
      @RequestParam(required = false) String status,
      PageParams page) {
    return service.list(productId, parentId, status, page);
  }

  @PutMapping("/{id}")
  public ProductModuleDetail update(
      @PathVariable Long id, @Valid @RequestBody ProductModuleUpdateRequest req) {
    return service.update(id, req);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
