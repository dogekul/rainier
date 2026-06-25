/* (C) 2026 Rainier — internal use only. */
package com.rainier.product.bootstrap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * v0.0.13 schema cleanup verification. Fresh H2 never contains the legacy v0.0.12 artifacts (the
 * ProductCategory entity is deleted, so ddl-auto never creates its table), so TC-LCLN-001 first
 * fabricates them manually and then asserts the runner removes both (slices.md 陷阱 K).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LegacyProductCategoryCleanupTest {

  @Autowired private LegacyProductCategoryCleanup cleanup;
  @PersistenceContext private EntityManager em;

  /** TC-LCLN-001: 假造 v0.0.12 遗留 schema → runner 清理 → 表与列均消失. */
  @Test
  void run_legacySchemaPresent_dropsTableAndColumn() {
    em.createNativeQuery(
            "CREATE TABLE IF NOT EXISTS rainier_product_category ("
                + "id BIGINT PRIMARY KEY, code VARCHAR(64))")
        .executeUpdate();
    em.createNativeQuery("ALTER TABLE rainier_product ADD COLUMN IF NOT EXISTS category_id BIGINT")
        .executeUpdate();
    assertTrue(tableExists("rainier_product_category"), "fixture table should exist");
    assertTrue(columnExists("rainier_product", "category_id"), "fixture column should exist");

    cleanup.run();

    assertFalse(tableExists("rainier_product_category"), "legacy table should be dropped");
    assertFalse(columnExists("rainier_product", "category_id"), "legacy column should be dropped");
  }

  /** TC-LCLN-002: schema 已干净时重复执行 → 无异常（idempotent no-op 分支). */
  @Test
  void run_alreadyClean_noopWithoutException() {
    assertFalse(tableExists("rainier_product_category"));
    assertFalse(columnExists("rainier_product", "category_id"));
    cleanup.run(); // must not throw
    assertFalse(tableExists("rainier_product_category"));
  }

  /** TC-LCLN-003: schema 表数（不含 product_category，含产品域 3 表 + 审计 + 里程碑）. */
  @Test
  @SuppressWarnings("unchecked")
  void schema_tableCount_withoutProductCategory() {
    List<Object> rows =
        em.createNativeQuery(
                "SELECT LOWER(TABLE_NAME) FROM INFORMATION_SCHEMA.TABLES"
                    + " WHERE LOWER(TABLE_NAME) LIKE 'rainier_%'")
            .getResultList();
    // v0.0.15 added rainier_audit_log (18); v0.0.17 added rainier_milestone → 19;
    // v0.0.31 added rainier_entity_link → 20; v0.0.43 added rainier_ai_work_log → 21;
    // v0.0.44 added rainier_opportunity + rainier_operation → 23;
    // v0.0.45 added rainier_opportunity_artifact → 24, + rainier_customer → 25 tables.
    // v0.0.58 added rainier_operation_issue → 26 tables.
    // v0.0.64 added rainier_organization_pmo + rainier_project_member → 28 tables.
    // v0.0.65 added rainier_event (flywheel event-pipeline base) → 29 tables.
    // v0.0.68 added rainier_ai_error (AI 错误公示板, A4) → 30 tables.
    // v0.0.69 added rainier_field_lock (AI 分级授权字段锁, A5) → 31 tables.
    assertEquals(31, rows.size(), "schema must have exactly 31 rainier_* tables: " + rows);
    assertTrue(rows.contains("rainier_operation_issue"));
    assertTrue(rows.contains("rainier_organization_pmo"));
    assertTrue(rows.contains("rainier_project_member"));
    assertTrue(rows.contains("rainier_event"));
    assertTrue(rows.contains("rainier_ai_error"));
    assertTrue(rows.contains("rainier_field_lock"));
    assertFalse(rows.contains("rainier_product_category"));
    assertTrue(rows.contains("rainier_product"));
    assertTrue(rows.contains("rainier_product_module"));
    assertTrue(rows.contains("rainier_feature"));
    assertTrue(rows.contains("rainier_sprint_feature"));
    assertTrue(rows.contains("rainier_audit_log"));
    assertTrue(rows.contains("rainier_milestone"));
  }

  private boolean tableExists(String table) {
    Number n =
        (Number)
            em.createNativeQuery(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES"
                        + " WHERE LOWER(TABLE_NAME) = :t")
                .setParameter("t", table.toLowerCase())
                .getSingleResult();
    return n.intValue() > 0;
  }

  private boolean columnExists(String table, String column) {
    Number n =
        (Number)
            em.createNativeQuery(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS"
                        + " WHERE LOWER(TABLE_NAME) = :t AND LOWER(COLUMN_NAME) = :c")
                .setParameter("t", table.toLowerCase())
                .setParameter("c", column.toLowerCase())
                .getSingleResult();
    return n.intValue() > 0;
  }
}
