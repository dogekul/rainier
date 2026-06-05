# 删除 Organization 的 isPmo 字段 — 技术设计

## Context

- **代码基线**：v0.0.4-id-migration（commit 1d948dc）
- **触发点**：v0.0.4 Gate 3 手测时识别到 PMO 是"人 × 岗位"概念，被错误地建模为组织节点的 `is_pmo: Boolean` 列
- **技术栈**：Spring Boot 2.7.18（Java 8）+ MySQL 8 + Hibernate JPA（ddl-auto=update，**Flyway 已禁用**）+ React 18 + Vite + TypeScript
- **运行时约束**：
  - `spring.jpa.hibernate.ddl-auto=update` 只会 **加列**，不会 drop 列。所以单纯删 entity 字段不会自动清掉数据库 `is_pmo` 列 — 需要外部动作。
  - Jackson 默认配置（`spring.jackson.deserialization.fail-on-unknown-properties` 未显式设为 true，即默认为 false）会 **静默忽略** 请求 body 中未知字段，提供天然的向后兼容。
- **范围保证**：本变更只删除一个错位字段；不引入岗位 / 角色实体（那是下个 demand-requirement 的事）。

## Decisions

### 1. 字段移除范围 — 全栈彻底清除

**方案**：从持久层到 UI 一次性删干净 — Java entity `isPmo` 字段 + `@Column` 注解 + getter/setter；3 个 DTO 的 `isPmo` 字段 + 配套 getter/setter；Service 中 3 处赋值（create 1 处 + update 2 处）；前端 TS 类型 3 处 + UI 表单复选框 + 列表列定义。

**为什么**：保留任何一处都会让"PMO 仍然属于组织"的错误模型继续渗漏。比如只删 UI 不删 entity → 数据库仍有列、API 仍能写、未来开发者会重复同样的错误；只删 entity 不删 DTO → 编译会失败。一次清干净是唯一一致的状态。

**备选方案及排除原因**：
- 备选 A — "渐进式 deprecate"（保留字段但加 `@Deprecated`，下个版本再删）：v0 阶段没有外部依赖，不需要 deprecate 期，纯增加复杂度。
- 备选 B — "保留 DB 列，仅删 entity + UI"：DB 留 dangling 列 + 未来文档负担；不接受。

### 2. 数据策略 — 方案 A：docker compose down -v 清卷重生（推荐）

**方案**：交付时执行 `docker compose down -v && docker compose up -d --build`，Hibernate ddl-auto 在空 schema 上重建表，自然不会有 `is_pmo` 列。

**为什么**：
- 与 v0.0.4-id-migration 完全一致的实践（v0 phase 无生产数据是共识）
- 不需要为 v0 引入"一次性数据迁移脚本"的额外机制
- Flyway 已禁用，没有可注入 ALTER 的位置
- 操作步骤简单、可重复、可在 CI 上原样跑

**备选方案及排除原因**：
- 备选 B — `docker exec rainier-mysql mysql -u rainier -p... -e 'ALTER TABLE rainier_organization DROP COLUMN is_pmo'`：保数据但引入 STDD 流程之外的手动 SQL 步骤；交付脚本要嵌入，污染流程；用户手测数据本来就是临时验证用，保不保都不影响后续。
- 备选 C — `data.sql` 注入 ALTER：Spring `data.sql` 在每次启动跑 → 第二次启动会因列已不存在报错；需要加 `IF EXISTS` 条件 + 列存在性检查 → 复杂度远超收益。

### 3. 向后兼容策略 — 请求 body 中残留的 isPmo 静默忽略

**方案**：不主动校验"客户端 POST/PUT body 中含 isPmo"为错误。Jackson 默认配置会忽略未知字段，response body 中自然不再含 isPmo。这就是契约层面的"删除"。

**为什么**：
- 短期（前端发布滞后于后端、缓存的旧版 SPA）防止 400 风暴
- 长期：前端一同发布，不会再发送 isPmo；旧字段在 wire format 上消失
- 不需要写"deprecated field" warning header — v0 没有外部消费者

**备选方案及排除原因**：
- 备选 A — 显式 `@JsonIgnoreProperties(ignoreUnknown = false)` + 400 拒绝：会让旧版前端的写请求直接挂掉；与 v0 实践冲突。

### 4. 主规范修订策略 — 在 ADDED 块内 in-place 编辑

**方案**：直接编辑 `specs/entity-organization/spec.md` 顶部的 ADDED 块（v0.0.3 写下的内容），删除 3 处 `isPmo` 提及（line 55 / 73 / 96）。不在末尾新增 "MODIFIED Requirements (from change remove-org-pmo)" 块。

**为什么**：
- 本变更是 **字段删除**，不是"add/modify a behavior"。Append-style 的 MODIFIED 块适合"原有 Requirement 改语义"，不适合"原有 Requirement 删一个字段"
- ADDED 块里那 3 行如果不删除，主规范就会自相矛盾："body SHALL 含 isPmo" vs "body 不含 isPmo"
- 归档原版（archive/2026-06-04-org-tree-and-employee/specs/entity-organization/spec.md）保留历史完整性 — 谁回看 v0.0.3 时序仍能看到 isPmo 曾存在

**备选方案及排除原因**：
- 备选 A — 末尾追加 "## REMOVED Requirements" + 列出删除条目：增加导航成本；规范文档不是 changelog
- 备选 B — 整篇 spec 推倒重写：丢失变更历史；对 review 不友好

### 5. 测试策略 — 删除旧 assertion + 新增 1 个负向断言 + 1 个前端 negative test

**方案**：
- 后端：删除 `OrganizationControllerCreateTest.java:67` 的 `andExpect(jsonPath("$.isPmo").value(false))`；在 1 个 create 测试中新增 `andExpect(jsonPath("$.isPmo").doesNotExist())` 作为正向回归保护；新增 1 个测试覆盖"PUT body 带 isPmo 静默忽略"
- 前端：新增 1 个 `EditDrawer.test.tsx` 测试 — assert `queryByLabelText('PMO 团队')` 为 null；新增 1 个 `OrganizationsPage.test.tsx` 或在已有 test 中 assert 表头不含 "PMO"
- E2E：交付前 `docker exec mysql ... DESCRIBE rainier_organization` 应不含 `is_pmo` 列

**为什么**：
- `doesNotExist()` 是 RED-able 的断言（如果有人不小心又把字段加回来就会失败）
- 前端组件级 negative test 比集成测试便宜且足够（保证 UI 不再渲染该控件）
- E2E DESCRIBE 是契约-级的兜底（防止"代码里删了但 DB 还有列"）

**备选方案及排除原因**：
- 备选 A — 完全不补新测试，只删旧 assertion：删除是 RED-friendly 的，但留不下回归保护
- 备选 B — 对所有 organization 端点 + 所有响应路径都加 doesNotExist：过度，1 个就够

### 6. Spec 双 capability 拆分 — entity-organization + frontend-scaffold

**方案**：本次只触动 2 个 capability。entity-organization 负责"实体上没有 isPmo + API 契约不含 isPmo"；frontend-scaffold 负责"UI 不渲染 PMO 控件"。

**为什么**：
- 这两个 capability 在 v0.0.4 已经存在，本次只是修订；不引入 NEW capability
- backend-scaffold / entity-user / entity-user-organization 完全不动 — 范围最小

## Architecture

```
┌─ DELETE 链路 ───────────────────────────────────────────────────┐
│                                                                  │
│  SQL              Java Entity        DTO                Service  │
│  ───              ────────────       ───                ───────  │
│  is_pmo  ─▶ DROP   Organization      OrgCreateRequest   create() │
│  COLUMN  by        .isPmo (delete)   .isPmo (delete)    setIsPmo │
│  down -v           @Column(...)      OrgUpdateRequest   (delete) │
│                                      .isPmo (delete)    update() │
│                                      OrgDetail          if/setIs │
│                                      .isPmo (delete)    Pmo×2    │
│                                                         (delete) │
│                                                                  │
│  React API        Frontend UI        Backend Test       Spec     │
│  ─────────        ────────────       ──────────────     ────     │
│  organization.ts  EditDrawer.tsx     CreateTest:67      entity-  │
│  Organization     PMO checkbox       jsonPath("$.is     org L55  │
│  Create.isPmo     + isPmo state      Pmo")  (delete)    L73 L96  │
│  Update.isPmo     + submit body                         (delete) │
│  (all delete)     (delete)           CreateTest         3 处删除  │
│                   OrganizationsPage  (新): does                   │
│                   "PMO" 列(delete)   NotExist 断言                │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘

         ▼ 交付时 ▼
┌─ Runtime 时序 ───────────────────────────────────────────────────┐
│ 1. git commit + tag v0.0.5-remove-org-pmo                       │
│ 2. docker compose down -v   ← 关键：清卷                         │
│ 3. docker compose up -d --build                                  │
│ 4. Hibernate ddl-auto=update 在空 schema 上 CREATE TABLE         │
│    rainier_organization (...) — 无 is_pmo 列                     │
│ 5. 验证：docker exec rainier-mysql mysql -e \                    │
│         "DESCRIBE rainier.rainier_organization" | grep -v is_pmo │
└──────────────────────────────────────────────────────────────────┘
```

## Risks / Trade-offs

| 风险 | 缓解措施 |
|---|---|
| Hibernate ddl-auto 不会 drop 列 → 如果用户跳过 `down -v` 直接 up，数据库残留 is_pmo 列 | 在交付 README + .stdd.yaml 中显式写明 down -v 是 mandatory；test-report 包含 DESCRIBE 校验 |
| 前端缓存的旧 SPA 仍发送 isPmo → 后端 400 风暴 | 已通过决策 3 兜底：Jackson 静默忽略未知字段；并在 E2E 中 curl POST body 带 isPmo 验证不报错 |
| 主规范 ADDED 块被原地编辑而非 append → 减少历史可追溯性 | archive/2026-06-04-org-tree-and-employee/specs/entity-organization/spec.md 保留 v0.0.3 原版完整内容 |
| Phase 5 漏掉 DESCRIBE 校验 → 提交后才发现 DB 残留列 | test-plan.md 明确 TC-RMP-E2E-001 + verify Step 1f 包含 DESCRIBE |
| user 手测过程中已在 EditDrawer 勾过 isPmo=true 的节点（数据残留） | down -v 一并清掉；如果用户选保留数据（备选 B）则需手动清表前再 ALTER |
| 用户当下使用的本机环境 PID 42918 占用 8080 端口 | 已知，沿用 `RAINIER_BACKEND_HOST_PORT=18080` 工作流，不在本变更范围内 |
