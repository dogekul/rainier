/* (C) 2026 Rainier — internal use only. */
package com.rainier.product.bootstrap;

import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * v0.0.13 restructure runner — removes the v0.0.12 ProductCategory layer from the live schema:
 *
 * <ol>
 *   <li>{@code DROP TABLE rainier_product_category} (entity deleted in v0.0.13).
 *   <li>{@code ALTER TABLE rainier_product DROP COLUMN category_id} (field removed from Product).
 * </ol>
 *
 * <p>Hibernate ddl-auto=update never drops unmapped tables/columns, so both removals must be
 * explicit. Probes INFORMATION_SCHEMA before each statement — idempotent: second boot finds the
 * schema already clean and logs a no-op line. IRREVERSIBLE by design (no production data existed
 * when v0.0.13 shipped).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 3)
public class LegacyProductCategoryCleanup implements CommandLineRunner {

  private static final Logger log = LoggerFactory.getLogger(LegacyProductCategoryCleanup.class);

  @PersistenceContext private EntityManager em;

  @Override
  @Transactional
  public void run(String... args) {
    boolean acted = false;
    if (legacyTableExists()) {
      em.createNativeQuery("DROP TABLE rainier_product_category").executeUpdate();
      log.info("LegacyProductCategoryCleanup: dropped legacy table rainier_product_category");
      acted = true;
    }
    if (legacyColumnExists()) {
      em.createNativeQuery("ALTER TABLE rainier_product DROP COLUMN category_id").executeUpdate();
      log.info(
          "LegacyProductCategoryCleanup: dropped legacy category_id column from rainier_product");
      acted = true;
    }
    if (!acted) {
      log.info("LegacyProductCategoryCleanup: no-op — schema already clean");
    }
  }

  /** Returns true iff table {@code rainier_product_category} exists in the live DB schema. */
  @SuppressWarnings("unchecked")
  private boolean legacyTableExists() {
    List<Object> rows =
        em.createNativeQuery(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES"
                    + " WHERE LOWER(TABLE_NAME) = 'rainier_product_category'")
            .getResultList();
    return !rows.isEmpty();
  }

  /** Returns true iff column {@code rainier_product.category_id} exists in the live DB schema. */
  @SuppressWarnings("unchecked")
  private boolean legacyColumnExists() {
    List<Object> rows =
        em.createNativeQuery(
                "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS"
                    + " WHERE LOWER(TABLE_NAME) = 'rainier_product'"
                    + " AND LOWER(COLUMN_NAME) = 'category_id'")
            .getResultList();
    return !rows.isEmpty();
  }
}
