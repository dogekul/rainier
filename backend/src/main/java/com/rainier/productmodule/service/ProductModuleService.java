/* (C) 2026 Rainier — internal use only. */
package com.rainier.productmodule.service;

import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.ConflictException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.feature.repository.FeatureRepository;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business operations for {@link ProductModule}. v0.0.13 self-referencing tree invariants:
 *
 * <ul>
 *   <li>{@code productId} immutable after creation; {@code parentId} mutable with strict reparent
 *       validation, ordered cross-product → cycle → depth (design Decision 7).
 *   <li>Code uniqueness: {@code (parentId, code)} composite; top-level scope ({@code parentId}
 *       null) enforced per-product at the service layer because the DB UQ skips NULL rows.
 *   <li>Tree depth capped by {@code rainier.product-module.depth.max} (root = 1, default 3).
 *   <li>Delete blocked bidirectionally: child Features first, then child modules.
 *   <li>List enrich is batched (slices.md 陷阱 J): page ancestors are fetched in ≤ depth-1 extra
 *       round-trips, never per-row.
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class ProductModuleService {

  private final ProductModuleRepository repo;
  private final ProductRepository productRepo;
  private final UserRepository userRepo;
  private final FeatureRepository featureRepo;

  private final int maxDepth;

  public ProductModuleService(
      ProductModuleRepository repo,
      ProductRepository productRepo,
      UserRepository userRepo,
      FeatureRepository featureRepo,
      @Value("${rainier.product-module.depth.max:3}") int maxDepth) {
    this.repo = repo;
    this.productRepo = productRepo;
    this.userRepo = userRepo;
    this.featureRepo = featureRepo;
    this.maxDepth = maxDepth;
  }

  @Transactional
  public ProductModuleDetail create(ProductModuleCreateRequest req) {
    if (!productRepo.existsById(req.getProductId())) {
      throw new BadRequestException("product not found: id=" + req.getProductId());
    }
    if (req.getParentId() != null) {
      ProductModule parent =
          repo.findById(req.getParentId())
              .orElseThrow(
                  () ->
                      new BadRequestException("parent module not found: id=" + req.getParentId()));
      if (!parent.getProductId().equals(req.getProductId())) {
        throw new BadRequestException("parent module must belong to the same product");
      }
      int newDepth = depthOf(parent) + 1;
      if (newDepth > maxDepth) {
        throw new BadRequestException(
            "max module depth exceeded: " + newDepth + " > " + maxDepth);
      }
    }
    if (!userRepo.existsById(req.getOwnerUserId())) {
      throw new BadRequestException("owner user not found: id=" + req.getOwnerUserId());
    }
    checkCodeUnique(req.getParentId(), req.getProductId(), req.getCode());
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
    m.setParentId(req.getParentId());
    m.setOwnerUserId(req.getOwnerUserId());
    return enrich(repo.saveAndFlush(m));
  }

  public ProductModuleDetail findById(Long id) {
    return enrich(getOrThrow(id));
  }

  public PageResponse<ProductModuleDetail> list(
      Long productId, Long parentId, String status, PageParams page) {
    Specification<ProductModule> spec =
        (root, query, cb) -> {
          javax.persistence.criteria.Predicate p = cb.conjunction();
          if (productId != null) {
            p = cb.and(p, cb.equal(root.get("productId"), productId));
          }
          if (parentId != null) {
            p = cb.and(p, cb.equal(root.get("parentId"), parentId));
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
        userIds.isEmpty() ? Collections.emptyMap() : batchById(userRepo.findAllById(userIds), User::getId);
    Map<Long, Product> productMap =
        productIds.isEmpty()
            ? Collections.emptyMap()
            : batchById(productRepo.findAllById(productIds), Product::getId);
    Map<Long, ProductModule> ancestorMap = batchAncestors(modules);
    return PageResponse.of(
        modules.stream()
            .map(m -> enrichBatch(m, userMap, productMap, ancestorMap))
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
    Long newParentId = req.getParentId();
    boolean parentChanged = !Objects.equals(newParentId, m.getParentId());
    if (parentChanged && newParentId != null) {
      ProductModule newParent =
          repo.findById(newParentId)
              .orElseThrow(
                  () -> new BadRequestException("parent module not found: id=" + newParentId));
      // Reparent validation order fixed by design Decision 7: cross-product → cycle → depth.
      if (!newParent.getProductId().equals(m.getProductId())) {
        throw new BadRequestException("parent module must belong to the same product");
      }
      if (newParentId.equals(id) || collectDescendants(id).contains(newParentId)) {
        throw new BadRequestException("cycle detected: would create ancestor loop");
      }
      int newDeepest = depthOf(newParent) + subtreeHeight(id);
      if (newDeepest > maxDepth) {
        throw new BadRequestException(
            "max module depth exceeded: " + newDeepest + " > " + maxDepth);
      }
    }
    boolean codeChanged = !req.getCode().equals(m.getCode());
    if (parentChanged || codeChanged) {
      checkCodeUnique(newParentId, m.getProductId(), req.getCode());
    }
    if (parentChanged) {
      m.setParentId(newParentId);
    }
    if (codeChanged) {
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
    if (repo.countByParentId(id) > 0) {
      throw new ConflictException("module has linked sub-modules");
    }
    repo.delete(m);
  }

  ProductModule getOrThrow(Long id) {
    return repo.findById(id)
        .orElseThrow(() -> new NotFoundException("product module not found: id=" + id));
  }

  /**
   * Composite code uniqueness (design Decision 3). NULL parent = top-level scope, unique within
   * the product at the service layer; non-null parent backed by the DB composite UQ.
   */
  private void checkCodeUnique(Long parentId, Long productId, String code) {
    if (parentId == null) {
      if (repo.existsByProductIdAndParentIdIsNullAndCode(productId, code)) {
        throw new ConflictException("code already exists in top-level: " + code);
      }
    } else {
      if (repo.existsByParentIdAndCode(parentId, code)) {
        throw new ConflictException("code already exists under parent: " + code);
      }
    }
  }

  /** Depth of {@code m} in the tree (top-level = 1) by walking the parent chain upward. */
  private int depthOf(ProductModule m) {
    int depth = 1;
    Long cur = m.getParentId();
    // maxDepth bounds the walk; +1 headroom lets the caller produce the exceeded message.
    while (cur != null && depth <= maxDepth + 1) {
      depth++;
      cur = repo.findById(cur).map(ProductModule::getParentId).orElse(null);
    }
    return depth;
  }

  /** Height of the subtree rooted at {@code rootId} (a leaf = 1), BFS via findByParentId. */
  private int subtreeHeight(Long rootId) {
    int height = 0;
    List<Long> level = Collections.singletonList(rootId);
    while (!level.isEmpty() && height <= maxDepth + 1) {
      height++;
      List<Long> next = new ArrayList<>();
      for (Long nodeId : level) {
        for (ProductModule child : repo.findByParentId(nodeId)) {
          next.add(child.getId());
        }
      }
      level = next;
    }
    return height;
  }

  /** All descendant ids of {@code rootId} (excluding itself), BFS (slices.md 陷阱 I). */
  private Set<Long> collectDescendants(Long rootId) {
    Set<Long> acc = new HashSet<>();
    LinkedList<Long> queue = new LinkedList<>();
    queue.add(rootId);
    while (!queue.isEmpty()) {
      Long cur = queue.poll();
      for (ProductModule child : repo.findByParentId(cur)) {
        if (acc.add(child.getId())) {
          queue.add(child.getId());
        }
      }
    }
    return acc;
  }

  /**
   * Fetches every ancestor of the page's modules in ≤ maxDepth-1 batched round-trips
   * (slices.md 陷阱 J) and returns them keyed by id, page rows included.
   */
  private Map<Long, ProductModule> batchAncestors(List<ProductModule> modules) {
    Map<Long, ProductModule> map = new HashMap<>();
    for (ProductModule m : modules) {
      map.put(m.getId(), m);
    }
    Set<Long> missing =
        modules.stream()
            .map(ProductModule::getParentId)
            .filter(Objects::nonNull)
            .filter(pid -> !map.containsKey(pid))
            .collect(Collectors.toCollection(HashSet::new));
    int rounds = 0;
    while (!missing.isEmpty() && rounds++ < maxDepth) {
      List<ProductModule> fetched = repo.findAllById(missing);
      Set<Long> nextMissing = new HashSet<>();
      for (ProductModule f : fetched) {
        map.put(f.getId(), f);
        if (f.getParentId() != null && !map.containsKey(f.getParentId())) {
          nextMissing.add(f.getParentId());
        }
      }
      missing = nextMissing;
    }
    return map;
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
    if (m.getParentId() != null) {
      repo.findById(m.getParentId())
          .ifPresent(
              parent -> {
                dto.setParentCode(parent.getCode());
                dto.setParentName(parent.getName());
              });
    }
    dto.setPathName(buildPath(m, this::resolveParent, ProductModule::getName));
    dto.setPathCodes(buildPath(m, this::resolveParent, ProductModule::getCode));
    return dto;
  }

  private ProductModule resolveParent(Long parentId) {
    return repo.findById(parentId).orElse(null);
  }

  private ProductModuleDetail enrichBatch(
      ProductModule m,
      Map<Long, User> userMap,
      Map<Long, Product> productMap,
      Map<Long, ProductModule> ancestorMap) {
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
    if (m.getParentId() != null) {
      ProductModule parent = ancestorMap.get(m.getParentId());
      if (parent != null) {
        dto.setParentCode(parent.getCode());
        dto.setParentName(parent.getName());
      }
    }
    dto.setPathName(buildPath(m, ancestorMap::get, ProductModule::getName));
    dto.setPathCodes(buildPath(m, ancestorMap::get, ProductModule::getCode));
    return dto;
  }

  /** Joins ancestor-chain labels root-first with " / ", e.g. {@code "钱包 / 余额"}. */
  private String buildPath(
      ProductModule m,
      Function<Long, ProductModule> parentResolver,
      Function<ProductModule, String> label) {
    LinkedList<String> parts = new LinkedList<>();
    ProductModule cur = m;
    int guard = 0;
    while (cur != null && guard++ <= maxDepth + 1) {
      parts.addFirst(label.apply(cur));
      cur = cur.getParentId() == null ? null : parentResolver.apply(cur.getParentId());
    }
    return String.join(" / ", parts);
  }

  private static <T> Map<Long, T> batchById(Iterable<T> entities, Function<T, Long> idExtractor) {
    Map<Long, T> map = new HashMap<>();
    for (T entity : entities) {
      map.put(idExtractor.apply(entity), entity);
    }
    return map;
  }
}
