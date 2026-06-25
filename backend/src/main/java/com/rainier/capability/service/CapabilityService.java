/* (C) 2026 Rainier — internal use only. */
package com.rainier.capability.service;

import com.rainier.capability.domain.CapabilityTag;
import com.rainier.capability.domain.UserCapability;
import com.rainier.capability.dto.CapabilityTagDto;
import com.rainier.capability.dto.CapabilityTagListResponse;
import com.rainier.capability.dto.UserCapabilityDto;
import com.rainier.capability.repository.CapabilityTagRepository;
import com.rainier.capability.repository.UserCapabilityRepository;
import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.ConflictException;
import com.rainier.common.exception.NotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * v0.0.85 (C5) — capability tag dictionary + per-user assessments. Both write paths go through here:
 * tag creation (admin-gated by {@link com.rainier.authz.AdminPaths} Tier A {@code /api/admin}) and
 * user self-assessment (token-gated, forced {@code source=SELF} in the controller).
 */
@Service
@Transactional(readOnly = true)
public class CapabilityService {

  /** Allowed tag categories. String column (not enum) — keep schema-flexible. */
  public static final Set<String> CATEGORIES =
      Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("TECH", "PRODUCT", "SOFT")));

  /** Allowed assessment sources. */
  public static final Set<String> SOURCES =
      Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("SELF", "MANAGER")));

  /** Stable display order for {@code byCategory} bucketing. */
  static final List<String> CATEGORY_ORDER =
      Collections.unmodifiableList(Arrays.asList("TECH", "PRODUCT", "SOFT"));

  private final CapabilityTagRepository tagRepo;
  private final UserCapabilityRepository userCapRepo;

  public CapabilityService(CapabilityTagRepository tagRepo, UserCapabilityRepository userCapRepo) {
    this.tagRepo = tagRepo;
    this.userCapRepo = userCapRepo;
  }

  public List<CapabilityTagDto> listAllTags() {
    List<CapabilityTag> all = tagRepo.findAll();
    all.sort(
        Comparator.comparing(
                (CapabilityTag t) -> indexOfCategory(t.getCategory()))
            .thenComparing(CapabilityTag::getName, Comparator.nullsLast(String::compareTo)));
    return all.stream().map(CapabilityTagDto::from).collect(Collectors.toList());
  }

  public CapabilityTagListResponse listAllTagsWithBuckets() {
    List<CapabilityTagDto> flat = listAllTags();
    CapabilityTagListResponse out = new CapabilityTagListResponse();
    out.setFlat(flat);
    Map<String, List<CapabilityTagDto>> byCat = new LinkedHashMap<String, List<CapabilityTagDto>>();
    for (String c : CATEGORY_ORDER) {
      byCat.put(c, new ArrayList<CapabilityTagDto>());
    }
    for (CapabilityTagDto d : flat) {
      String cat = d.getCategory();
      if (cat == null) {
        continue;
      }
      List<CapabilityTagDto> bucket = byCat.get(cat);
      if (bucket == null) {
        // Unknown category snuck in — append a fresh bucket rather than dropping the row.
        bucket = new ArrayList<CapabilityTagDto>();
        byCat.put(cat, bucket);
      }
      bucket.add(d);
    }
    out.setByCategory(byCat);
    return out;
  }

  private static int indexOfCategory(String category) {
    if (category == null) {
      return Integer.MAX_VALUE;
    }
    int idx = CATEGORY_ORDER.indexOf(category);
    return idx < 0 ? Integer.MAX_VALUE - 1 : idx;
  }

  @Transactional
  public CapabilityTagDto createTag(String name, String category) {
    if (name == null || name.trim().isEmpty()) {
      throw new BadRequestException("name is required");
    }
    if (!CATEGORIES.contains(category)) {
      throw new BadRequestException("invalid category: " + category);
    }
    String trimmed = name.trim();
    if (tagRepo.existsByName(trimmed)) {
      throw new ConflictException("capability tag name already exists: " + trimmed);
    }
    CapabilityTag t = new CapabilityTag();
    t.setName(trimmed);
    t.setCategory(category);
    return CapabilityTagDto.from(tagRepo.saveAndFlush(t));
  }

  public List<UserCapabilityDto> listUserCapabilities(Long userId) {
    if (userId == null) {
      return Collections.emptyList();
    }
    List<UserCapability> rows = userCapRepo.findByUserId(userId);
    if (rows.isEmpty()) {
      return Collections.emptyList();
    }
    List<Long> tagIds =
        rows.stream().map(UserCapability::getCapabilityTagId).collect(Collectors.toList());
    Map<Long, CapabilityTag> tagMap = new HashMap<Long, CapabilityTag>();
    for (CapabilityTag t : tagRepo.findAllById(tagIds)) {
      tagMap.put(t.getId(), t);
    }
    List<UserCapabilityDto> out = new ArrayList<UserCapabilityDto>();
    for (UserCapability uc : rows) {
      CapabilityTag tag = tagMap.get(uc.getCapabilityTagId());
      if (tag == null) {
        continue; // tag soft-deleted; drop dangling row from the read-model
      }
      UserCapabilityDto d = new UserCapabilityDto();
      d.setId(uc.getId());
      d.setUserId(uc.getUserId());
      d.setCapabilityTagId(uc.getCapabilityTagId());
      d.setTagName(tag.getName());
      d.setTagCategory(tag.getCategory());
      d.setLevel(uc.getLevel());
      d.setSource(uc.getSource());
      out.add(d);
    }
    out.sort(
        Comparator.comparing(
                (UserCapabilityDto d) -> indexOfCategory(d.getTagCategory()))
            .thenComparing(UserCapabilityDto::getTagName, Comparator.nullsLast(String::compareTo)));
    return out;
  }

  @Transactional
  public UserCapabilityDto setUserCapability(
      Long userId, Long tagId, Integer level, String source) {
    if (userId == null) {
      throw new BadRequestException("userId is required");
    }
    if (tagId == null) {
      throw new BadRequestException("capabilityTagId is required");
    }
    if (level == null || level < 1 || level > 5) {
      throw new BadRequestException("level must be in [1,5]");
    }
    if (!SOURCES.contains(source)) {
      throw new BadRequestException("invalid source: " + source);
    }
    CapabilityTag tag =
        tagRepo
            .findById(tagId)
            .orElseThrow(
                () -> new NotFoundException("capability tag not found: id=" + tagId));
    Optional<UserCapability> existing = userCapRepo.findByUserIdAndCapabilityTagId(userId, tagId);
    UserCapability uc = existing.orElseGet(UserCapability::new);
    uc.setUserId(userId);
    uc.setCapabilityTagId(tagId);
    uc.setLevel(level);
    uc.setSource(source);
    UserCapability saved = userCapRepo.saveAndFlush(uc);
    UserCapabilityDto d = new UserCapabilityDto();
    d.setId(saved.getId());
    d.setUserId(saved.getUserId());
    d.setCapabilityTagId(saved.getCapabilityTagId());
    d.setTagName(tag.getName());
    d.setTagCategory(tag.getCategory());
    d.setLevel(saved.getLevel());
    d.setSource(saved.getSource());
    return d;
  }
}
