/* (C) 2026 Rainier — internal use only. */
package com.rainier.productcategory.service;

import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.ConflictException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.productcategory.domain.ProductCategory;
import com.rainier.productcategory.domain.ProductCategoryStatus;
import com.rainier.productcategory.dto.ProductCategoryCreateRequest;
import com.rainier.productcategory.dto.ProductCategoryDetail;
import com.rainier.productcategory.dto.ProductCategoryUpdateRequest;
import com.rainier.product.repository.ProductRepository;
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
 * Business operations for {@link ProductCategory}. Flat — no parent FK. v0.0.12 invariants:
 *
 * <ul>
 *   <li>{@code code} service-level unique.
 *   <li>Owner mutable (family Decision 6b).
 *   <li>Soft-deleted; FK protection on delete (Product references) — wired in M09.
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class ProductCategoryService {

  private final ProductCategoryRepository repo;
  private final UserRepository userRepo;
  private final ProductRepository productRepo;

  public ProductCategoryService(
      ProductCategoryRepository repo, UserRepository userRepo, ProductRepository productRepo) {
    this.repo = repo;
    this.userRepo = userRepo;
    this.productRepo = productRepo;
  }

  @Transactional
  public ProductCategoryDetail create(ProductCategoryCreateRequest req) {
    if (!userRepo.existsById(req.getOwnerUserId())) {
      throw new BadRequestException("owner user not found: id=" + req.getOwnerUserId());
    }
    if (repo.existsByCode(req.getCode())) {
      throw new ConflictException("code already exists: " + req.getCode());
    }
    String status = req.getStatus() == null ? ProductCategoryStatus.ACTIVE : req.getStatus();
    if (!ProductCategoryStatus.ALL.contains(status)) {
      throw new BadRequestException("invalid status: " + status);
    }
    ProductCategory c = new ProductCategory();
    c.setCode(req.getCode());
    c.setName(req.getName());
    c.setDescription(req.getDescription());
    c.setStatus(status);
    c.setOwnerUserId(req.getOwnerUserId());
    return enrich(repo.saveAndFlush(c));
  }

  public ProductCategoryDetail findById(Long id) {
    return enrich(getOrThrow(id));
  }

  public PageResponse<ProductCategoryDetail> list(String status, PageParams page) {
    Specification<ProductCategory> spec =
        (root, query, cb) -> {
          javax.persistence.criteria.Predicate p = cb.conjunction();
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
    Page<ProductCategory> result = repo.findAll(spec, pr);
    List<ProductCategory> categories = result.getContent();
    if (categories.isEmpty()) {
      return PageResponse.of(
          Collections.emptyList(), page.getPage(), page.getSize(), result.getTotalElements());
    }
    Set<Long> userIds =
        categories.stream()
            .map(ProductCategory::getOwnerUserId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(HashSet::new));
    Map<Long, User> userMap =
        userIds.isEmpty() ? Collections.emptyMap() : batchUserById(userRepo.findAllById(userIds));
    return PageResponse.of(
        categories.stream().map(c -> enrichBatch(c, userMap)).collect(Collectors.toList()),
        page.getPage(),
        page.getSize(),
        result.getTotalElements());
  }

  @Transactional
  public ProductCategoryDetail update(Long id, ProductCategoryUpdateRequest req) {
    ProductCategory c = getOrThrow(id);
    if (!ProductCategoryStatus.ALL.contains(req.getStatus())) {
      throw new BadRequestException("invalid status: " + req.getStatus());
    }
    if (!Objects.equals(req.getOwnerUserId(), c.getOwnerUserId())) {
      if (!userRepo.existsById(req.getOwnerUserId())) {
        throw new BadRequestException("owner user not found: id=" + req.getOwnerUserId());
      }
      c.setOwnerUserId(req.getOwnerUserId());
    }
    if (!req.getCode().equals(c.getCode())) {
      if (repo.existsByCode(req.getCode())) {
        throw new ConflictException("code already exists: " + req.getCode());
      }
      c.setCode(req.getCode());
    }
    c.setName(req.getName());
    c.setDescription(req.getDescription());
    c.setStatus(req.getStatus());
    return enrich(repo.saveAndFlush(c));
  }

  @Transactional
  public void delete(Long id) {
    ProductCategory c = getOrThrow(id);
    if (productRepo.countByCategoryId(id) > 0) {
      throw new ConflictException("category has linked products");
    }
    repo.delete(c);
  }

  ProductCategory getOrThrow(Long id) {
    return repo.findById(id)
        .orElseThrow(() -> new NotFoundException("product category not found: id=" + id));
  }

  private ProductCategoryDetail enrich(ProductCategory c) {
    ProductCategoryDetail dto = ProductCategoryDetail.from(c);
    userRepo
        .findById(c.getOwnerUserId())
        .ifPresent(
            u -> {
              dto.setOwnerName(u.getName());
              dto.setOwnerLoginName(u.getLoginName());
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

  private ProductCategoryDetail enrichBatch(ProductCategory c, Map<Long, User> userMap) {
    ProductCategoryDetail dto = ProductCategoryDetail.from(c);
    User u = userMap.get(c.getOwnerUserId());
    if (u != null) {
      dto.setOwnerName(u.getName());
      dto.setOwnerLoginName(u.getLoginName());
    }
    return dto;
  }
}
