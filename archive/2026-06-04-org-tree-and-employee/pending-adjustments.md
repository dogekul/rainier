# Pending Design Adjustments (Phase 4 BUILD)

> 长程模式下，实现过程中发现的小/大设计偏离自动记录于此；Phase 5 VERIFY 时汇总到 `design-adjustments.md`。

---

## Adjustment #1 — Flyway 8.5.13 不支持 MySQL 8 community；改用 Hibernate ddl-auto=update（涉及 design.md §2、§6）

**原始设计**：
- design.md §2：`flyway-core` + `flyway-mysql` 9.x；启动时自动 migrate
- spec backend-scaffold "Flyway 启动应用 V1__init_org.sql 迁移"

**实际实现（SLICE-Z01 E2E 验证时发现）**：
- Spring Boot 2.7.18 管理的 `flyway-core 8.5.13` 在 community edition 下拒绝 MySQL 8.0：
  `org.flywaydb.core.api.FlywayException: Unsupported Database: MySQL 8.0`
- Flyway 9+ 解决但需要 Java 11+（与本项目 Java 8 约束冲突）
- 尝试覆盖至 Flyway 7.15.x —— 该 patch 版本在 Maven Central 不可用
- **最终方案**：禁用 Flyway（`spring.flyway.enabled=false`），将 `spring.jpa.hibernate.ddl-auto` 改为 `update`，让 Hibernate 从 entity 注解生成 schema
- `V1__init_org.sql` 文件保留在 `backend/src/main/resources/db/migration/` 作为目标 schema 的文档与未来 Java 17 + Flyway 重启用时的起点

**影响范围**：
- E2E 启动现在依赖 Hibernate 生成 schema 而非 Flyway 迁移
- TC-BES-201（Flyway 应用 V1）spec 文本与实际行为不一致 —— 实际验证改为"MySQL 中存在 3 张表 + Hibernate 生成"，无 `flyway_schema_history`
- TC-DRT-201 spec 中"含 flyway_schema_history" 一句已不成立
- 未来引入业务表时（如 Project / Story / Task），schema 演进风险加大（Hibernate auto-update 在新增列时风险可控，但删/改列易出错）

**未来跟进**：
- 升级到 Java 17 + Spring Boot 3 + Flyway 9 时重新启用 Flyway，把 `V1__init_org.sql` 作为 baseline
- 或本变更接受当前方案，在引入业务实体表前评估是否提前升级 Java

**分类**：大偏离（流程层 + 行为可观测维度有变化，但产品契约不变）

---

## Adjustment #2 — AuditorAware Bean 从 B02 提前到 B01（涉及 slices.md）

**原始切片设计**：
- B01：pom + configs + `@EnableJpaAuditing`
- B02：BaseEntity + AuditorAwareImpl

**实际实现**：
- B01 加 `@EnableJpaAuditing(auditorAwareRef="auditorAware")` 后，Spring 启动时即查找名为 `auditorAware` 的 Bean，否则 contextLoads 失败
- 解决方案：把 `AuditorAwareImpl` 的实现提前到 B01；B02 只剩 `BaseEntity`

**影响范围**：
- B01 / B02 切片边界轻微移动
- 不影响最终代码结构、行为或测试用例
- 不影响任何 TC 的可验证结果

**分类**：小偏离（切片划分调整，无对外行为变化）

---

## Adjustment #3 — V1 SQL `COLLATE=utf8mb4_0900_ai_ci` 子句移除（涉及 V1__init_org.sql）

**原始设计**：MySQL 8 默认 collation `utf8mb4_0900_ai_ci`，所有表显式 `COLLATE=utf8mb4_0900_ai_ci`。

**实际实现**：移除每表 `COLLATE` 子句，让连接默认 collation 生效。这一改动是为了让 V1 SQL 在 MariaDB 上也能跑（事后未采用 MariaDB，但改动保留以增强可移植性）。

**影响范围**：
- 实际 collation 仍由 MySQL 8 默认（`utf8mb4_0900_ai_ci`）提供
- 无可观测行为差异
- V1 SQL 当前未在 dev 中运行（见 Adjustment #1），所以本项主要影响未来 Flyway 重启用时

**分类**：小偏离（增强可移植性）

---

## Adjustment #4 — docker-compose `RAINIER_MYSQL_*` 环境变量补全（涉及 docker-compose.yml）

**原始设计**：backend 仅设 `SPRING_PROFILES_ACTIVE=dev`；通过 default localhost 假设连接 MySQL。

**实际实现**：dev profile 默认 `localhost`，但在 Docker 内 backend 看不到 mysql 容器（同名 service），需显式：
```
RAINIER_MYSQL_HOST: mysql
RAINIER_MYSQL_PORT: "3306"
RAINIER_MYSQL_DATABASE: rainier
RAINIER_MYSQL_USERNAME: root
RAINIER_MYSQL_PASSWORD: rainier_root
```

**影响范围**：docker-compose.yml 多 5 行；本地直接 `mvn spring-boot:run` 不受影响（继续使用默认 localhost）。

**分类**：小偏离（部署配置缺位补齐）

---
