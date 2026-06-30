# Proposal — project-organization-edge (H1, v0.0.108)

## 命门
C-路线图 #4：Project 现在虽已有 `organizationId` 列（v0.0.28 起列就位、v0.0.64 起 create 默认注入
owner 主组织），但仍缺两块拼图：
1. 列表/查询端无法按 organizationId 过滤——领域/部门负责人字面无法「寻址我领域的项目」；
2. v0.0.28 之前历史 Project（以及任何 owner 主组织缺失/后补的）organizationId 仍 NULL，没有
   一次性回填路径，新的 organizationId 过滤会漏数。

H1 一次性补齐：GET 过滤 + 启动幂等回填。解锁 H3 与未来 4 个组织级角色寻址。

## 范围（实际增量）
1. **ProjectController**：`GET /api/projects` 新增 `organizationId` 可选 query param；
   ProjectService.list 沿用 Specification 拼 `cb.equal(root.get("organizationId"), organizationId)`。
2. **NEW `ProjectOrgBackfill`**（`com.rainier.project.bootstrap`, `@Component implements CommandLineRunner`）：
   - flag-gated `app.migration.project-org-backfill.enabled`（默认 true；test profile flips false）
   - 遍历所有 `organizationId IS NULL` 的 Project：
     - 找 `ownerUserId` 的 primary user_organization（`is_primary=1 AND left_at IS NULL`）取第一条
     - 沿 `parentId` 上溯到第一个 `type IN (DEPARTMENT, DOMAIN, COMPANY)`（即非 TEAM/SUBGROUP，
       「非 GROUP」按现 OrganizationType 的小组/团队映射）
     - 写入 `organizationId`
   - idempotent：只处理 NULL 行；找不到主组织/上溯无果 → 留空（下次启动重试）
   - log 一行 `backfilled N rows`（与 `ProjectTypeBackfill` 一致风格）
3. **NEW `changes/2026-06-25-project-organization-edge/spec.md`**
4. **测试**：
   - `ProjectControllerOrgFilterTest`：seed 3 项目挂不同 org → GET `?organizationId=X` 仅返回挂 X 的；不传该 param 不影响既有行为。
   - `ProjectOrgBackfillTest`：seed `organizationId=null` 项目 + 主组织在 SUBGROUP→TEAM→DEPARTMENT 链上 → backfill 后 organizationId = 第一个 DEPARTMENT 节点 id；幂等（再跑无变化）；非 NULL 不动。

## OutOfScope
- 多组织（项目 N:M）— 单 org 即可，后续可升 N:M
- ScopeService.resolveProjectIds 扩展（H2/H3 用到时再扩）
- 前端项目编辑 UI 暴露 organizationId（v0.0.64 已暴露「负责团队」字段；后续 H1.1 单独做领域/部门下拉）
- ProjectCreateRequest / ProjectUpdateRequest 已含 organizationId（v0.0.28），无需新增字段
- ProjectDetail 富化 organizationName/organizationType 已存在（v0.0.64），无需新增

## commit
`feat(project-organization-edge): H1 Project 列表按 organizationId 过滤 + 回填 (v0.0.108)`
