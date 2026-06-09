/* (C) 2026 Rainier — internal use only. */
package com.rainier.productcategory.controller;

import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.productcategory.dto.ProductCategoryCreateRequest;
import com.rainier.productcategory.dto.ProductCategoryDetail;
import com.rainier.productcategory.dto.ProductCategoryUpdateRequest;
import com.rainier.productcategory.service.ProductCategoryService;
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
@RequestMapping("/api/product-categories")
public class ProductCategoryController {

  private final ProductCategoryService service;

  public ProductCategoryController(ProductCategoryService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<ProductCategoryDetail> create(
      @Valid @RequestBody ProductCategoryCreateRequest req) {
    ProductCategoryDetail created = service.create(req);
    return ResponseEntity.created(URI.create("/api/product-categories/" + created.getId()))
        .body(created);
  }

  @GetMapping("/{id}")
  public ProductCategoryDetail get(@PathVariable Long id) {
    return service.findById(id);
  }

  @GetMapping
  public PageResponse<ProductCategoryDetail> list(
      @RequestParam(required = false) String status, PageParams page) {
    return service.list(status, page);
  }

  @PutMapping("/{id}")
  public ProductCategoryDetail update(
      @PathVariable Long id, @Valid @RequestBody ProductCategoryUpdateRequest req) {
    return service.update(id, req);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
