/* (C) 2026 Rainier — internal use only. */
package com.rainier.productmodule.service;

import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.ConflictException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.feature.repository.FeatureRepository;
import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.product.domain.Product;
import com.rainier.product.repository.ProductRepository;
import com.rainier.productmodule.domain.ProductModule;
import com.rainier.productmodule.domain.ProductModuleStatus;
import com.rainier.productmodule.dto.ProductModuleCreateRequest;
import com.rainier.productmodule.dto.ProductModuleDetail;
import com.rainier.productmodule.dto.ProductModuleUpdateRequest;
import com.rainier.productmodule.repository.ProductModuleRepository;
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
 * Business operations for {@link ProductModule}.
 *
 * <ul>
 *   <li>{@code productId} immutable after creation.
 *   <li>{@code code} service-level unique.
 *   <li>Owner mutable.
 *   <li>Soft-deleted; FK protection on delete (Feature references) — wired in M09.
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class ProductModuleService {

  private final ProductModuleRepository repo;
  private final ProductRepository productRepo;
  private final UserRepository userRepo;
  private final FeatureRepository featureRepo;

  public ProductModuleService(
      ProductModuleRepository repo,
      ProductRepository productRepo,
      UserRepository userRepo,
      FeatureRepository featureRepo) {
    this.repo = repo;
    this.productRepo = productRepo;
    this.userRepo = userRepo;
    this.featureRepo = featureRepo;
  }

  @Transactional
  public ProductModuleDetail create(ProductModuleCreateRequest req) {
    if (!productRepo.existsById(req.getProductId())) {
      throw new BadRequestException("product not found: id=" + req.getProductId());
    }
    if (!userRepo.existsById(req.getOwnerUserId())) {
      throw new BadRequestException("owner user not found: id=" + req.getOwnerUserId());
    }
    if (repo.existsByCode(req.getCode())) {
      throw new ConflictException("code already exists: " + req.getCode());
    }
    String status = req.getStatus() == null ? ProductModuleStatus.PLANNING : req.getStatus();
    if (!ProductModuleStatus.ALL.contains(status)) {
      throw new BadRequestException("invalid status: " + status);
    }
    ProductModule m = new ProductModule();
    m.setCode(req.getCode());
    m.setName(req.getName());
    m.setDescription(req.getDescription());
    m.setStatus(status);
    m.setProductId(req.getProductId());
    m.setOwnerUserId(req.getOwnerUserId());
    return enrich(repo.saveAndFlush(m));
  }

  public ProductModuleDetail findById(Long id) {
    return enrich(getOrThrow(id));
  }

  public PageResponse<ProductModuleDetail> list(Long productId, String status, PageParams page) {
    Specification<ProductModule> spec =
        (root, query, cb) -> {
          javax.persistence.criteria.Predicate p = cb.conjunction();
          if (productId != null) {
            p = cb.and(p, cb.equal(root.get("productId"), productId));
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
    Page<ProductModule> result = repo.findAll(spec, pr);
    List<ProductModule> modules = result.getContent();
    if (modules.isEmpty()) {
      return PageResponse.of(
          Collections.emptyList(), page.getPage(), page.getSize(), result.getTotalElements());
    }
    Set<Long> userIds =
        modules.stream()
            .map(ProductModule::getOwnerUserId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(HashSet::new));
    Set<Long> productIds =
        modules.stream()
            .map(ProductModule::getProductId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(HashSet::new));
    Map<Long, User> userMap =
        userIds.isEmpty() ? Collections.emptyMap() : batchUserById(userRepo.findAllById(userIds));
    Map<Long, Product> productMap =
        productIds.isEmpty()
            ? Collections.emptyMap()
            : batchProductById(productRepo.findAllById(productIds));
    return PageResponse.of(
        modules.stream()
            .map(m -> enrichBatch(m, userMap, productMap))
            .collect(Collectors.toList()),
        page.getPage(),
        page.getSize(),
        result.getTotalElements());
  }

  @Transactional
  public ProductModuleDetail update(Long id, ProductModuleUpdateRequest req) {
    ProductModule m = getOrThrow(id);
    if (!ProductModuleStatus.ALL.contains(req.getStatus())) {
      throw new BadRequestException("invalid status: " + req.getStatus());
    }
    if (!Objects.equals(req.getOwnerUserId(), m.getOwnerUserId())) {
      if (!userRepo.existsById(req.getOwnerUserId())) {
        throw new BadRequestException("owner user not found: id=" + req.getOwnerUserId());
      }
      m.setOwnerUserId(req.getOwnerUserId());
    }
    if (!req.getCode().equals(m.getCode())) {
      if (repo.existsByCode(req.getCode())) {
        throw new ConflictException("code already exists: " + req.getCode());
      }
      m.setCode(req.getCode());
    }
    m.setName(req.getName());
    m.setDescription(req.getDescription());
    m.setStatus(req.getStatus());
    // productId intentionally NOT touched — immutable after creation.
    return enrich(repo.saveAndFlush(m));
  }

  @Transactional
  public void delete(Long id) {
    ProductModule m = getOrThrow(id);
    if (featureRepo.countByModuleId(id) > 0) {
      throw new ConflictException("module has linked features");
    }
    repo.delete(m);
  }

  ProductModule getOrThrow(Long id) {
    return repo.findById(id)
        .orElseThrow(() -> new NotFoundException("product module not found: id=" + id));
  }

  private ProductModuleDetail enrich(ProductModule m) {
    ProductModuleDetail dto = ProductModuleDetail.from(m);
    userRepo
        .findById(m.getOwnerUserId())
        .ifPresent(
            u -> {
              dto.setOwnerName(u.getName());
              dto.setOwnerLoginName(u.getLoginName());
            });
    productRepo
        .findById(m.getProductId())
        .ifPresent(
            p -> {
              dto.setProductCode(p.getCode());
              dto.setProductName(p.getName());
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

  private static Map<Long, Product> batchProductById(Iterable<Product> products) {
    Map<Long, Product> map = new HashMap<>();
    for (Product p : products) {
      map.put(p.getId(), p);
    }
    return map;
  }

  private ProductModuleDetail enrichBatch(
      ProductModule m, Map<Long, User> userMap, Map<Long, Product> productMap) {
    ProductModuleDetail dto = ProductModuleDetail.from(m);
    User u = userMap.get(m.getOwnerUserId());
    if (u != null) {
      dto.setOwnerName(u.getName());
      dto.setOwnerLoginName(u.getLoginName());
    }
    Product p = productMap.get(m.getProductId());
    if (p != null) {
      dto.setProductCode(p.getCode());
      dto.setProductName(p.getName());
    }
    return dto;
  }
}
