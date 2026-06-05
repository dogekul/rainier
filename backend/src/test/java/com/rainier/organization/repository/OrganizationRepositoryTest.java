/* (C) 2026 Rainier — internal use only. */
package com.rainier.organization.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.rainier.organization.domain.Organization;
import com.rainier.organization.domain.OrganizationType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for {@link OrganizationRepository}.
 *
 * <p>Covers TC-BES-203 (soft delete pattern: DELETE → UPDATE, findById empty).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrganizationRepositoryTest {

  @Autowired private OrganizationRepository repo;

  @Test
  void save_assignsUuidIdAndAuditFields() {
    Organization o = newOrg("ROOT-1", "总公司一");
    Organization saved = repo.saveAndFlush(o);

    assertThat(saved.getId()).isNotNull().hasSize(32);
    assertThat(saved.getCreateTime()).isNotNull();
    assertThat(saved.getUpdateTime()).isNotNull();
    assertThat(saved.getDelFlag()).isFalse();
    assertThat(saved.getCreateBy()).isEqualTo("system");
  }

  @Test
  void delete_softDeletesViaSqlDelete_andFindByIdReturnsEmpty() {
    Organization o = repo.saveAndFlush(newOrg("ROOT-2", "总公司二"));
    String id = o.getId();

    repo.delete(o);
    repo.flush();

    // 1. JpaRepository sees no row because @Where filters
    assertThat(repo.findById(id)).isEmpty();
    // 2. Native count confirms row still exists physically
    assertThat(repo.countRawById(id)).isEqualTo(1L);
    // 3. del_flag was flipped to 1
    Object delFlag = repo.rawDelFlag(id);
    assertThat(delFlag).isNotNull();
    boolean isDeleted =
        delFlag instanceof Boolean ? (Boolean) delFlag : ((Number) delFlag).intValue() != 0;
    assertThat(isDeleted).isTrue();
  }

  @Test
  void existsByParentIdIsNullAndCode_findsRootByCode() {
    repo.saveAndFlush(newOrg("ROOT-3", "总公司三"));

    assertThat(repo.existsByParentIdIsNullAndCode("ROOT-3")).isTrue();
    assertThat(repo.existsByParentIdIsNullAndCode("NOPE")).isFalse();
  }

  private static Organization newOrg(String code, String name) {
    Organization o = new Organization();
    o.setType(OrganizationType.COMPANY);
    o.setCode(code);
    o.setName(name);
    o.setPath("/will-update-after-id");
    o.setWholeName(name);
    return o;
  }
}
