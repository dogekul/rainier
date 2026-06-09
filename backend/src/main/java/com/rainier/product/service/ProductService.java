/* (C) 2026 Rainier — internal use only. */
package com.rainier.product.service;

import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.ConflictException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.product.domain.Product;
import com.rainier.product.domain.ProductStatus;
import com.rainier.product.dto.ProductCreateRequest;
import com.rainier.product.dto.ProductDetail;
import com.rainier.product.dto.ProductUpdateRequest;
import com.rainier.product.repository.ProductRepository;
import com.rainier.productmodule.repository.ProductModuleRepository;
import com.rainier.productcategory.domain.ProductCategory;
import com.rainier.productcategory.repository.ProductCategoryRepository;
import com.rainier.user.domain.User;
import com.rainier.user.repository.UserRepository;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business operations for {@link Product}.
 *
 * <ul>
 *   <li>{@code categoryId} immutable after creation (spec Decision 11 sibling).
 *   <li>{@code code} service-level unique.
 *   <li>Owner mutable (family Decision 6b).
 *   <li>Soft-deleted; FK protection on delete (Module references) — wired in M09.
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class ProductService {

  private final ProductRepository repo;
  private final ProductCategoryRepository categoryRepo;
  private final UserRepository userRepo;
  private final ProductModuleRepository moduleRepo;

  public ProductService(
      ProductRepository repo,
      ProductCategoryRepository categoryRepo,
      UserRepository userRepo,
      ProductModuleRepository moduleRepo) {
    this.repo = repo;
    this.categoryRepo = categoryRepo;
    this.userRepo = userRepo;
    this.moduleRepo = moduleRepo;
  }

  @Transactional
  public ProductDetail create(ProductCreateRequest req) {
    if (!categoryRepo.existsById(req.getCategoryId())) {
      throw new BadRequestException("category not found: id=" + req.getCategoryId());
    }
    if (!userRepo.existsById(req.getOwnerUserId())) {
      throw new BadRequestException("owner user not found: id=" + req.getOwnerUserId());
    }
    if (repo.existsByCode(req.getCode())) {
      throw new ConflictException("code already exists: " + req.getCode());
    }
    String status = req.getStatus() == null ? ProductStatus.PLANNING : req.getStatus();
    if (!ProductStatus.ALL.contains(status)) {
      throw new BadRequestException("invalid status: " + status);
    }
    Product p = new Product();
    p.setCode(req.getCode());
    p.setName(req.getName());
    p.setDescription(req.getDescription());
    p.setStatus(status);
    p.setCategoryId(req.getCategoryId());
    p.setOwnerUserId(req.getOwnerUserId());
    return enrich(repo.saveAndFlush(p));
  }

  public ProductDetail findById(Long id) {
    return enrich(getOrThrow(id));
  }

  public PageResponse<ProductDetail> list(Long categoryId, String status, PageParams page) {
    Specification<Product> spec =
        (root, query, cb) -> {
          javax.persistence.criteria.Predicate p = cb.conjunction();
          if (categoryId != null) {
            p = cb.and(p, cb.equal(root.get("categoryId"), categoryId));
          }
          if (status != null) {
            p = cb.and(p, cb.equal(root.get("status"), status));
          }
          String search = page.getSearch();
          if (search != null && !search.isEmpty()) {
            String pattern = "%" + search.toLowerCase() + "%";
            p =
                cb.and(
                    p,
                    cb.or(
                        cb.like(cb.lower(root.get("code")), pattern),
                        cb.like(cb.lower(root.get("name")), pattern)));
          }
          return p;
        };
    PageRequest pr =
        PageRequest.of(page.getPage(), page.getSize(), Sort.by(Sort.Direction.DESC, "createTime"));
    Page<Product> result = repo.findAll(spec, pr);
    List<Product> products = result.getContent();
    if (products.isEmpty()) {
      return PageResponse.of(
          Collections.emptyList(), page.getPage(), page.getSize(), result.getTotalElements());
    }
    Set<Long> userIds =
        products.stream()
            .map(Product::getOwnerUserId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(HashSet::new));
    Set<Long> categoryIds =
        products.stream()
            .map(Product::getCategoryId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(HashSet::new));
    Map<Long, User> userMap =
        userIds.isEmpty() ? Collections.emptyMap() : batchUserById(userRepo.findAllById(userIds));
    Map<Long, ProductCategory> categoryMap =
        categoryIds.isEmpty()
            ? Collections.emptyMap()
            : batchCategoryById(categoryRepo.findAllById(categoryIds));
    return PageResponse.of(
        products.stream()
            .map(p -> enrichBatch(p, userMap, categoryMap))
            .collect(Collectors.toList()),
        page.getPage(),
        page.getSize(),
        result.getTotalElements());
  }

  @Transactional
  public ProductDetail update(Long id, ProductUpdateRequest req) {
    Product p = getOrThrow(id);
    if (!ProductStatus.ALL.contains(req.getStatus())) {
      throw new BadRequestException("invalid status: " + req.getStatus());
    }
    if (!Objects.equals(req.getOwnerUserId(), p.getOwnerUserId())) {
      if (!userRepo.existsById(req.getOwnerUserId())) {
        throw new BadRequestException("owner user not found: id=" + req.getOwnerUserId());
      }
      p.setOwnerUserId(req.getOwnerUserId());
    }
    if (!req.getCode().equals(p.getCode())) {
      if (repo.existsByCode(req.getCode())) {
        throw new ConflictException("code already exists: " + req.getCode());
      }
      p.setCode(req.getCode());
    }
    p.setName(req.getName());
    p.setDescription(req.getDescription());
    p.setStatus(req.getStatus());
    // categoryId intentionally NOT touched — immutable after creation.
    return enrich(repo.saveAndFlush(p));
  }

  @Transactional
  public void delete(Long id) {
    Product p = getOrThrow(id);
    if (moduleRepo.countByProductId(id) > 0) {
      throw new ConflictException("product has linked modules");
    }
    repo.delete(p);
  }

  Product getOrThrow(Long id) {
    return repo.findById(id)
        .orElseThrow(() -> new NotFoundException("product not found: id=" + id));
  }

  private ProductDetail enrich(Product p) {
    ProductDetail dto = ProductDetail.from(p);
    userRepo
        .findById(p.getOwnerUserId())
        .ifPresent(
            u -> {
              dto.setOwnerName(u.getName());
              dto.setOwnerLoginName(u.getLoginName());
            });
    categoryRepo
        .findById(p.getCategoryId())
        .ifPresent(
            c -> {
              dto.setCategoryCode(c.getCode());
              dto.setCategoryName(c.getName());
            });
    return dto;
  }

  private static Map<Long, User> batchUserById(Iterable<User> users) {
    Map<Long, User> map = new HashMap<>();
    for (User u : users) {
      map.put(u.getId(), u);
    }
    return map;
  }

  private static Map<Long, ProductCategory> batchCategoryById(Iterable<ProductCategory> cats) {
    Map<Long, ProductCategory> map = new HashMap<>();
    for (ProductCategory c : cats) {
      map.put(c.getId(), c);
    }
    return map;
  }

  private ProductDetail enrichBatch(
      Product p, Map<Long, User> userMap, Map<Long, ProductCategory> categoryMap) {
    ProductDetail dto = ProductDetail.from(p);
    User u = userMap.get(p.getOwnerUserId());
    if (u != null) {
      dto.setOwnerName(u.getName());
      dto.setOwnerLoginName(u.getLoginName());
    }
    ProductCategory c = categoryMap.get(p.getCategoryId());
    if (c != null) {
      dto.setCategoryCode(c.getCode());
      dto.setCategoryName(c.getName());
    }
    return dto;
  }
}
