# 设计调整说明 — 组织维度骨架

> 原始设计基线：Phase 2 产出的 design.md + specs/*.md + test-plan.md
> 调整来源：Phase 3-5 实现过程中的发现
> 完整原文：见 `pending-adjustments.md`

## 调整汇总

| # | 调整类型 | 涉及文档 | 严重程度 | 调整阶段 | 用户已知 |
|---|---|---|---|---|---|
| 1 | 技术方案变更 + spec 行为修订 | design.md §2 / specs/{backend-scaffold, dev-runtime} / test-plan TC-BES-201, TC-DRT-201 | Major | Phase 5（Z01 E2E 触发）| 是 |
| 2 | 切片边界调整 | slices.md B01/B02 | Minor | Phase 4 | 是 |
| 3 | V1 SQL 移除 COLLATE 子句 | V1__init_org.sql | Minor | Phase 5 | 是 |
| 4 | docker-compose 补全 RAINIER_MYSQL_* 环境变量 | docker-compose.yml | Minor | Phase 5 | 是 |
| 5 | 文档同步：test-plan TC-BES-201 / TC-DRT-201 spec 文本更新 | test-plan.md | Minor | Phase 5 | 是 |

## 调整详细说明

### 调整 1：Flyway 禁用，改由 Hibernate ddl-auto=update 生成 schema （Major）

- **原始设计**：design.md §2 锁定 `flyway-core 9.x + flyway-mysql`，启动自动 `migrate`；spec backend-scaffold "Flyway 启动应用 V1__init_org.sql 迁移"；spec dev-runtime "MySQL 含 4 张表（含 flyway_schema_history）"
- **调整内容**：
  - 实际加载的 `flyway-core 8.5.13`（Spring Boot 2.7.18 管理版本）在 community edition 下拒绝 MySQL 8.0：`FlywayException: Unsupported Database: MySQL 8.0`
  - 尝试 Flyway 7.15.x 覆盖 → 该 patch 版本不存在
  - Flyway 9+ 修复了此问题但要求 Java 11+，与本项目 Java 8 约束冲突
  - **最终方案**：`spring.flyway.enabled=false`，`spring.jpa.hibernate.ddl-auto=update`；Hibernate 从 entity 注解自动生成 schema；`V1__init_org.sql` 保留作为目标 schema 的文档与未来 JDK 升级时的 baseline
- **调整原因**：在 Phase 5 E2E 验证（SLICE-Z01）启动 backend 时触发；MySQL 8 + Flyway 8.x community 是已知冲突，本变更无法以 Java 8 解
- **影响范围**：
  - spec backend-scaffold "Flyway 启动应用 V1__init_org.sql" 不成立 → test-plan TC-BES-201 已更新为"Hibernate 生成 schema"
  - spec dev-runtime "MySQL 含 4 张表" 不成立 → test-plan TC-DRT-201 已更新为"3 业务表，无 flyway_schema_history"
  - 业务行为（API、CRUD、软删、级联）完全不受影响；前后端契约不变
  - 未来引入业务实体（Project / Story 等）时 schema 演进有风险（Hibernate auto-update 加列安全，改/删列不安全）
- **跟进**：升级 Java 17 + Spring Boot 3 + Flyway 9 时重新启用，以 `V1__init_org.sql` 为 baseline
- **用户已知**：是（pending-adjustments.md #1 + design-adjustments.md #1 + test-plan 已修订）

### 调整 2：AuditorAware Bean 从 B02 提前到 B01（Minor）

- **原始设计**：slices.md B01 "pom + configs + @EnableJpaAuditing"；B02 "BaseEntity + AuditorAware"
- **调整内容**：B01 添加 `@EnableJpaAuditing(auditorAwareRef="auditorAware")` 后，Spring 启动期立即查找该 Bean。把 `AuditorAwareImpl` 实现提前到 B01；B02 只剩 BaseEntity。
- **调整原因**：B01 若不带 AuditorAwareImpl，`contextLoads` 直接失败（`NoSuchBeanDefinitionException: auditorAware`）
- **影响范围**：B01/B02 切片边界轻微移动；最终代码结构、行为、测试结果无差异
- **用户已知**：是（pending-adjustments.md #2）

### 调整 3：V1 SQL 移除 `COLLATE=utf8mb4_0900_ai_ci` 子句（Minor）

- **原始设计**：每表显式 `COLLATE=utf8mb4_0900_ai_ci`（MySQL 8 默认）
- **调整内容**：移除每表 `COLLATE` 子句，让连接默认 collation 生效，使 V1 SQL 可移植到 MariaDB（尝试过 MariaDB 但因网络问题未采用，改动保留）
- **影响范围**：连接到 MySQL 8 时仍是 `utf8mb4_0900_ai_ci`；无可观测行为差异；V1 SQL 当前未在 dev 中运行（见调整 1）
- **用户已知**：是（pending-adjustments.md #3）

### 调整 4：docker-compose 补全 RAINIER_MYSQL_* 环境变量（Minor）

- **原始设计**：backend service 只设 `SPRING_PROFILES_ACTIVE=dev`；依赖 application-dev.yml 默认值
- **调整内容**：补全 5 个 env：`RAINIER_MYSQL_HOST=mysql`、`RAINIER_MYSQL_PORT=3306`、`RAINIER_MYSQL_DATABASE=rainier`、`RAINIER_MYSQL_USERNAME=root`、`RAINIER_MYSQL_PASSWORD=rainier_root`
- **调整原因**：dev profile 默认 `localhost`，容器内 backend 看到的 localhost 是自己；需指向 service 名 `mysql`
- **影响范围**：docker-compose.yml +5 行；本地 `mvn spring-boot:run` 走默认 localhost 不受影响
- **用户已知**：是（pending-adjustments.md #4）

### 调整 5：test-plan TC-BES-201 / TC-DRT-201 spec 文本同步（Minor）

- **原始 spec 文本**：
  - TC-BES-201: "Flyway 启动应用 V1 + flyway_schema_history 含 success=true 行"
  - TC-DRT-201: "MySQL 含 4 表... flyway_schema_history"
- **调整内容**：两条 TC 的预期结果更新为 Hibernate 自动生成 schema 路径；明确"无 flyway_schema_history 表"为已知现状
- **调整原因**：与调整 1 同步，避免 spec 文本与实际行为长期错位
- **影响范围**：test-plan.md 局部文字；不动 specs/*.md（spec 文本保留为"目标态"作为未来重启用 Flyway 的需求基线，由调整 1 在 design-adjustments 与 pending-adjustments 中说明）
- **用户已知**：是（本文档）

## 结论

5 项调整中 1 项 Major（Flyway 禁用）、4 项 Minor；全部已记录、用户已知、不破坏产品契约；可进入 Phase 6 交付。Major 调整需要在未来 Java 17 升级时回评。
