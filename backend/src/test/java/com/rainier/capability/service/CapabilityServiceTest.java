/* (C) 2026 Rainier — internal use only. */
package com.rainier.capability.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rainier.capability.dto.CapabilityTagDto;
import com.rainier.capability.dto.CapabilityTagListResponse;
import com.rainier.capability.dto.UserCapabilityDto;
import com.rainier.capability.repository.CapabilityTagRepository;
import com.rainier.capability.repository.UserCapabilityRepository;
import com.rainier.common.exception.BadRequestException;
import com.rainier.common.exception.ConflictException;
import com.rainier.common.exception.NotFoundException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** v0.0.85 (C5) — covers TC-CAP-001..010. */
@SpringBootTest
@ActiveProfiles("test")
class CapabilityServiceTest {

  @Autowired private CapabilityService service;
  @Autowired private CapabilityTagRepository tagRepo;
  @Autowired private UserCapabilityRepository userCapRepo;

  @BeforeEach
  void clean() {
    userCapRepo.deleteAll();
    tagRepo.deleteAll();
  }

  /** TC-CAP-001: createTag 正常 → 返回带 id 的 dto，listAll 含之。 */
  @Test
  void createTag_happy() {
    CapabilityTagDto t = service.createTag("Java", "TECH");
    assertNotNull(t.getId());
    assertEquals("Java", t.getName());
    assertEquals("TECH", t.getCategory());
    List<CapabilityTagDto> all = service.listAllTags();
    assertEquals(1, all.size());
    assertEquals("Java", all.get(0).getName());
  }

  /** TC-CAP-002: createTag 重名 → ConflictException. */
  @Test
  void createTag_duplicateName_conflict() {
    service.createTag("Java", "TECH");
    assertThrows(ConflictException.class, () -> service.createTag("Java", "TECH"));
  }

  /** TC-CAP-003: createTag category 非法 → BadRequestException. */
  @Test
  void createTag_badCategory() {
    assertThrows(BadRequestException.class, () -> service.createTag("X", "BAD"));
  }

  /** TC-CAP-004: setUserCapability 首次 → 新建. */
  @Test
  void setUserCapability_first_insert() {
    CapabilityTagDto t = service.createTag("Java", "TECH");
    UserCapabilityDto uc = service.setUserCapability(7L, t.getId(), 3, "SELF");
    assertNotNull(uc.getId());
    assertEquals(7L, uc.getUserId().longValue());
    assertEquals(3, uc.getLevel().intValue());
    assertEquals("SELF", uc.getSource());
    assertEquals("Java", uc.getTagName());
    assertEquals(1, userCapRepo.count());
  }

  /** TC-CAP-005: setUserCapability 再次同 tag → 更新（level/source 变；不新增行）. */
  @Test
  void setUserCapability_second_update() {
    CapabilityTagDto t = service.createTag("Java", "TECH");
    UserCapabilityDto first = service.setUserCapability(7L, t.getId(), 3, "SELF");
    UserCapabilityDto second = service.setUserCapability(7L, t.getId(), 5, "MANAGER");
    assertEquals(first.getId(), second.getId(), "must update in place, not insert");
    assertEquals(5, second.getLevel().intValue());
    assertEquals("MANAGER", second.getSource());
    assertEquals(1, userCapRepo.count());
  }

  /** TC-CAP-006: level=0 或 6 → BadRequestException. */
  @Test
  void setUserCapability_levelOutOfRange() {
    CapabilityTagDto t = service.createTag("Java", "TECH");
    assertThrows(
        BadRequestException.class, () -> service.setUserCapability(1L, t.getId(), 0, "SELF"));
    assertThrows(
        BadRequestException.class, () -> service.setUserCapability(1L, t.getId(), 6, "SELF"));
  }

  /** TC-CAP-007: source=OTHER → BadRequestException. */
  @Test
  void setUserCapability_badSource() {
    CapabilityTagDto t = service.createTag("Java", "TECH");
    assertThrows(
        BadRequestException.class, () -> service.setUserCapability(1L, t.getId(), 3, "OTHER"));
  }

  /** TC-CAP-008: tagId 不存在 → NotFoundException. */
  @Test
  void setUserCapability_tagMissing() {
    assertThrows(
        NotFoundException.class, () -> service.setUserCapability(1L, 99999L, 3, "SELF"));
  }

  /** TC-CAP-009: listUserCapabilities 返回 join 后的 dto. */
  @Test
  void listUserCapabilities_joinedShape() {
    CapabilityTagDto t1 = service.createTag("Java", "TECH");
    CapabilityTagDto t2 = service.createTag("沟通", "SOFT");
    service.setUserCapability(42L, t1.getId(), 4, "SELF");
    service.setUserCapability(42L, t2.getId(), 2, "SELF");
    List<UserCapabilityDto> list = service.listUserCapabilities(42L);
    assertEquals(2, list.size());
    for (UserCapabilityDto d : list) {
      assertNotNull(d.getTagName());
      assertNotNull(d.getTagCategory());
    }
    // Other user untouched.
    assertTrue(service.listUserCapabilities(99L).isEmpty());
  }

  /** TC-CAP-010: categorizeTags 按 TECH/PRODUCT/SOFT 分桶且 each 内有序. */
  @Test
  void categorize_buckets() {
    service.createTag("Java", "TECH");
    service.createTag("Frontend", "TECH");
    service.createTag("用户研究", "PRODUCT");
    service.createTag("沟通", "SOFT");
    CapabilityTagListResponse resp = service.listAllTagsWithBuckets();
    Map<String, List<CapabilityTagDto>> by = resp.getByCategory();
    assertTrue(by.containsKey("TECH"));
    assertTrue(by.containsKey("PRODUCT"));
    assertTrue(by.containsKey("SOFT"));
    assertEquals(2, by.get("TECH").size());
    assertEquals(1, by.get("PRODUCT").size());
    assertEquals(1, by.get("SOFT").size());
    // TECH bucket alphabetical by name.
    List<CapabilityTagDto> tech = by.get("TECH");
    assertEquals("Frontend", tech.get(0).getName());
    assertEquals("Java", tech.get(1).getName());
    assertFalse(resp.getFlat().isEmpty());
  }
}
