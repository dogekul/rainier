/* (C) 2026 Rainier — internal use only. */
package com.rainier.organization.repository;

import com.rainier.organization.domain.Organization;
import com.rainier.organization.domain.OrganizationType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link Organization}.
 *
 * <p>All derived queries inherit the entity's {@code @Where("del_flag = 0")} clause and therefore
 * skip soft-deleted rows. Use {@link #countRawById(Long)} (native) when you need to confirm
 * physical presence including soft-deletes — currently used only by tests.
 */
@Repository
public interface OrganizationRepository
    extends JpaRepository<Organization, Long>, JpaSpecificationExecutor<Organization> {

  boolean existsByParentIdAndCode(Long parentId, String code);

  boolean existsByParentIdIsNullAndCode(String code);

  long countByParentId(Long parentId);

  List<Organization> findByParentId(Long parentId);

  Optional<Organization> findByPathAndName(String path, String name);

  List<Organization> findAllByOrderByPathAsc();

  List<Organization> findByType(OrganizationType type);

  /**
   * Native count that bypasses {@code @Where} — for tests asserting soft-delete leaves the row in
   * place.
   */
  @Query(value = "SELECT COUNT(*) FROM rainier_organization WHERE id = :id", nativeQuery = true)
  long countRawById(@Param("id") Long id);

  /**
   * Native fetch of {@code del_flag} bypassing {@code @Where}. Returns {@code Object} because the
   * driver maps {@code TINYINT(1)} to {@code Boolean} under H2 (MySQL mode); on real MySQL the
   * driver maps it to either {@code Boolean} or {@code Integer} depending on {@code tinyInt1isBit},
   * but {@code Boolean.TRUE.equals(...)} works for both.
   */
  @Query(value = "SELECT del_flag FROM rainier_organization WHERE id = :id", nativeQuery = true)
  Object rawDelFlag(@Param("id") Long id);
}
