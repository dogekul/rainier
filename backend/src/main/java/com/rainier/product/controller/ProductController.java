/* (C) 2026 Rainier — internal use only. */
package com.rainier.product.controller;

import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.product.dto.ProductCreateRequest;
import com.rainier.product.dto.ProductDetail;
import com.rainier.product.dto.ProductUpdateRequest;
import com.rainier.product.service.ProductService;
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
@RequestMapping("/api/products")
public class ProductController {

  private final ProductService service;

  public ProductController(ProductService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<ProductDetail> create(@Valid @RequestBody ProductCreateRequest req) {
    ProductDetail created = service.create(req);
    return ResponseEntity.created(URI.create("/api/products/" + created.getId())).body(created);
  }

  @GetMapping("/{id}")
  public ProductDetail get(@PathVariable Long id) {
    return service.findById(id);
  }

  @GetMapping
  public PageResponse<ProductDetail> list(
      @RequestParam(required = false) String status, PageParams page) {
    return service.list(status, page);
  }

  @PutMapping("/{id}")
  public ProductDetail update(
      @PathVariable Long id, @Valid @RequestBody ProductUpdateRequest req) {
    return service.update(id, req);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
