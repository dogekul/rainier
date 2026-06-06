/* (C) 2026 Rainier — internal use only. */
package com.rainier.position.service;

import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.ConflictException;
import com.rainier.common.exception.NotFoundException;
import com.rainier.common.web.PageParams;
import com.rainier.common.web.PageResponse;
import com.rainier.position.domain.Position;
import com.rainier.position.domain.PositionCategory;
import com.rainier.position.dto.PositionCreateRequest;
import com.rainier.position.dto.PositionDetail;
import com.rainier.position.dto.PositionUpdateRequest;
import com.rainier.position.repository.PositionRepository;
import com.rainier.user.repository.UserRepository;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business operations for {@link Position}. */
@Service
@Transactional(readOnly = true)
public class PositionService {

  private final PositionRepository repo;
  private final UserRepository userRepo;

  public PositionService(PositionRepository repo, UserRepository userRepo) {
    this.repo = repo;
    this.userRepo = userRepo;
  }

  @Transactional
  public PositionDetail create(PositionCreateRequest req) {
    if (!PositionCategory.ALL.contains(req.getCategory())) {
      throw new BadRequestException("invalid category: " + req.getCategory());
    }
    if (repo.existsByCode(req.getCode())) {
      throw new ConflictException("code already exists: " + req.getCode());
    }
    Position p = new Position();
    p.setCode(req.getCode());
    p.setName(req.getName());
    p.setDescription(req.getDescription());
    p.setCategory(req.getCategory());
    p.setEnabled(req.getEnabled() == null ? Boolean.TRUE : req.getEnabled());
    return PositionDetail.from(repo.saveAndFlush(p));
  }

  public PositionDetail findById(Long id) {
    return PositionDetail.from(getOrThrow(id));
  }

  public PageResponse<PositionDetail> list(String category, Boolean enabled, PageParams page) {
    Specification<Position> spec =
        (root, query, cb) -> {
          javax.persistence.criteria.Predicate p = cb.conjunction();
          if (category != null) {
            p = cb.and(p, cb.equal(root.get("category"), category));
          }
          if (enabled != null) {
            p = cb.and(p, cb.equal(root.get("enabled"), enabled));
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
    PageRequest req =
        PageRequest.of(page.getPage(), page.getSize(), Sort.by(Sort.Direction.DESC, "createTime"));
    Page<Position> result = repo.findAll(spec, req);
    return PageResponse.of(
        result.stream().map(PositionDetail::from).collect(Collectors.toList()),
        page.getPage(),
        page.getSize(),
        result.getTotalElements());
  }

  @Transactional
  public PositionDetail update(Long id, PositionUpdateRequest req) {
    Position p = getOrThrow(id);
    if (!PositionCategory.ALL.contains(req.getCategory())) {
      throw new BadRequestException("invalid category: " + req.getCategory());
    }
    p.setName(req.getName());
    if (req.getDescription() != null) {
      p.setDescription(req.getDescription());
    }
    p.setCategory(req.getCategory());
    if (req.getEnabled() != null) {
      p.setEnabled(req.getEnabled());
    }
    return PositionDetail.from(repo.saveAndFlush(p));
  }

  @Transactional
  public void delete(Long id) {
    Position p = getOrThrow(id);
    long userCount = userRepo.countByPositionId(id);
    if (userCount > 0) {
      throw new ConflictException("position has assigned users");
    }
    repo.delete(p);
  }

  Position getOrThrow(Long id) {
    return repo.findById(id)
        .orElseThrow(() -> new NotFoundException("position not found: id=" + id));
  }
}
