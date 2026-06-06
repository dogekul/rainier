# Project 实体 + 激活 2 占位 FK — 技术设计

## Context

- **代码基线**：v0.0.7-position-role（commit 9894eff）— 9 张表（org/user/uo/demand/requirement/demand_requirement/position/role/user_role）+ 单 BaseEntity + BIGINT IDENTITY + 状态 enum=VARCHAR + 富化 enrich 模式都已稳定
- **技术栈**：Spring Boot 2.7.18（Java 8）+ MySQL 8 + Hibernate JPA（ddl-auto=update，**Flyway 已禁用**）+ React 18 + Vite + TypeScript + Zustand + Axios
- **已就绪复用资产**：
  - `BaseEntity` + `AuditorAwareImpl`（已自动注入 createBy/updateBy = JWT 当前 username）
  - 异常体系：`BadRequestException` / `ConflictException` / `NotFoundException` / `GlobalExceptionHandler`
  - 已建立的 enum 字符串校验模式（`Set<String> ALL` + Service.contains 校验）
  - 已建立的"占位 FK 字段位 + 后续 retrofit 校验"模式（v0.0.6/v0.0.7 注释中明示）
  - 已建立的"软删 entity FK delete 保护"模式
  - 已建立的 enrich + 容错模式（SourceDemandView / UserDetail.positionName / UserRoleDetail.userName）
  - 前端：`useAuthStore.user.username` + listX 池查询 + 默认选中匹配
- **当前 DB 占位脏数据**：
  - `user_role.id=2` 有 `projectId=42`（v0.0.7 测试时填的，无对应 Project）
  - `requirement` 表无 project_id 引用（content=[]）
- **约束**：
  - 不引入新 Maven / npm 依赖
  - 不动 v0.0.7 任何已交付 entity（包括 schema 不动）
  - 不写 V4 SQL 历史档（沿用 v0.0.5/v0.0.6/v0.0.7 教训）
  - **不 docker compose down -v**；保现有手测数据（alice / lili / 后台开发 / PMO / YFM / 2 行 user_role）

## Decisions

### 1. 角色配置归宿 — Project 只 owner_user_id，hat 全走 UserRole

**方案**：Project 表里只放一个"人字段" `owner_user_id`（项目负责人 / 总责任人 / 创建期 PM）。所有"项目维度的角色 hat"（PM / PMO / TechLead / QA Lead / Architect / Reviewer / Designer / ...）全部走 v0.0.7 引入的 UserRole M2M (user × role × project)。

**为什么**：
- v0.0.7 引入 Role + UserRole 体系的本意就是承载"项目维度的角色 hat"。再在 Project 表加 pm_user_id / pmo_user_id 等于把 v0.0.7 推翻一半
- 业务加新 hat（如"Data Lead"）→ admin 在 /hr/roles 加一条 = 全栈解锁，零 schema 改动
- 一人多 hat / 一 hat 多人天然支持
- 历史可追：每条 user_role 都是独立行 + 审计字段

**备选方案及排除原因**：
- 备选 A — Project 表加固定字段 pm_user_id / pmo_user_id / tech_lead_user_id：v0.0.5 删 Org.isPmo 的同款反模式；字段膨胀；硬约束太死
- 备选 B — 新建 project_member M2M（user × project，无 role 维度）：成员关系无角色信息；与已有 UserRole 重复

### 2. owner_user_id 业务字段 — 可改

**方案**：`Project.owner_user_id` 是业务必填字段（@NotNull），写在 `ProjectCreateRequest` / `ProjectUpdateRequest` 两个 DTO 里。Create 时 service 校验存在；Update 时若传新值且与原值不同，重新校验存在然后赋值。

**为什么**：
- owner 是项目的本质属性（创建者 / 总责任人）— 显式必填比"框架自动从 JWT 推"更清晰
- 项目生命周期长（vs Requirement 是短期产物），owner 转移是常见操作 — 应该允许 PUT 改
- 与 v0.0.6 `Requirement.ownerUserId`（不可改）不同 — 不同实体不同选择，无需对齐
- createBy/updateBy 是审计字段，由 `AuditorAwareImpl` 自动注入登录 username — **两层概念清晰分开**

**备选方案及排除原因**：
- 备选 A — owner 不可改（同 Requirement）：项目长期化场景下转 owner = 删项目重建，UX 差
- 备选 B — owner 从 JWT 自动推（不在 DTO 里）：admin 创建项目代他人当 owner 的场景做不了；混淆"创建者"和"owner"

### 3. 状态 enum — VARCHAR + Service 校验

**方案**：`status VARCHAR(16) NN`；常量类 `ProjectStatus` 含 `PLANNING / ACTIVE / ON_HOLD / DELIVERED / ARCHIVED` + `Set<String> ALL`；Service create/update 时 `ALL.contains(status)` 校验，非法 → BadRequestException。默认 `PLANNING`。

**为什么**：与 v0.0.6 DemandStatus / RequirementStatus / v0.0.7 PositionCategory 完全一致；保持柔性（业务加新状态零代码改动）。

### 4. code 唯一性 — Service 层 only

**方案**：`code VARCHAR(64) NN`，**不加 DB UNIQUE**；Service create 时 `existsByCode` 校验。

**为什么**：与 v0.0.6 Requirement.code / v0.0.7 Position.code / Role.code 完全一致；DB UNIQUE 会与 soft-delete reuse 语义冲突（v0.0.6 KL 已明示）。

### 5. FK 删除保护 — Requirement + UserRole 双向检查

**方案**：`DELETE /api/projects/{id}` 校验两条：
- `requirementRepo.countByProjectId(id) > 0` → 409 `"project has linked requirements"`
- `userRoleRepo.countByProjectId(id) > 0` → 409 `"project has assigned user-roles"`

**为什么**：与 v0.0.3 Organization → UserOrganization、v0.0.6 Demand/Requirement → DemandRequirementLink、v0.0.7 Position → User、Role → UserRole 的 FK 保护模式完全对齐。

**实现**：
- 给 `RequirementRepository` 加 `countByProjectId(Long)` 派生查询
- 给 `UserRoleRepository` 加 `countByProjectId(Long)` 派生查询
- `ProjectService` 注入这两个 repo

### 6. Requirement / UserRole 占位 FK 激活 — Service 校验 + Detail 富化 + 启动时自愈（严格）

**方案**：
- `RequirementService.create / update` 在 `req.getProjectId() != null` 时调 `projectRepo.existsById`；不存在 → BadRequestException `"project not found"`
- `UserRoleService.create` 同款（NULL 保留公司级 hat 语义不校验）
- `RequirementDetail` / `UserRoleDetail` 加 `projectName` + `projectCode` 字段
- 新增 `DanglingProjectIdCleanup`（Spring `CommandLineRunner`）— 应用启动时执行：
  ```java
  @Component
  public class DanglingProjectIdCleanup implements CommandLineRunner {
    @Override @Transactional
    public void run(String... args) {
      cleanDangling("rainier_requirement", "project_id");
      cleanDangling("rainier_user_role", "project_id");
    }
    private void cleanDangling(String table, String column) {
      // native UPDATE table SET column = NULL
      //   WHERE column IS NOT NULL
      //     AND column NOT IN (SELECT id FROM rainier_project WHERE del_flag = 0);
      // log WARN per row cleaned
    }
  }
  ```
- `RequirementService.enrich()` / `UserRoleService.enrich()` 直接 `projectRepo.findById(projectId)` → 假设必然找到（DanglingProjectIdCleanup 已保证）；找不到 → null（防御性兜底，不抛错；但实际启动后理论不会出现）

**为什么**：
- v0.0.6/v0.0.7 注释明示「v0.0.8 Project 落地后做数据清理 + 加校验」— 兑现 + 强化承诺
- 严格化哲学：v0.0.8 之后没有 dangling 数据存在；Service 假设 projectId 非空时 Project 必存在
- 自愈清理是一次性的（第一次启动清干净；后续 noop）
- UI 无侵害：app 启动后 reads 永远干净 + projectName 永远有值（除非 projectId 本来就 null）

**注意 trade-off**：
- 自愈走 native UPDATE 绕过 @SQLDelete 软删生命周期 — 仅此一处例外，且是单向操作（NULL out）
- log WARN 是 admin 唯一感知（看 docker logs）；不发邮件 / 不阻塞启动

**备选方案及排除原因**：
- 备选 A — reads 富化容错（返 null 不报错）：实际上是"假装严格"；脏数据持续存在让 Gate 1 关于"strict"承诺名不副实
- 备选 B — reads 严格 throw exception：UI 挂掉；admin 必须手动清才能恢复
- 备选 C — Phase 4 写 V4 SQL migration：违反 v0.0.5/v0.0.6 "不写 SQL 历史档"决策

### 6b. Requirement.ownerUserId 改为可改 — v0.0.6 不可改决策的对内修订

**方案**：v0.0.6 `RequirementUpdateRequest` 无 ownerUserId 字段（Jackson 静默丢弃），service 不接受改 owner。本次：
- `RequirementUpdateRequest` 加 `ownerUserId` 字段（@NotNull）
- `RequirementService.update` 接收，若与现 owner 不同 → 校验存在 → 赋值
- 前端 `RequirementEditDrawer` 把 ownerUserId 下拉的 `disabled` 移除（编辑时也可改）

**为什么**：
- 与 Project.owner 可改保持一致（同款决策推进到 Requirement）
- 业务实际场景：需求可能转给另一个 PO（人员调动 / 责任转移）
- v0.0.6 TC-REQ-007 "PUT ownerUserId 静默忽略" 语义反转 — 在主规范 entity-requirement MODIFIED 块用 1 行说明

**备选方案及排除原因**：
- 备选 A — 保持 Requirement.owner 不可改：与 Project.owner 不一致；用户已显式要求统一

### 7. ProjectDetail 富化 owner_user — Service.enrich 注入 UserRepository

**方案**：`ProjectDetail` 加 `ownerName` + `ownerLoginName` 字段；`ProjectService.enrich` 按 ownerUserId 查 User 仓库设置富化字段。

**为什么**：与 v0.0.7 UserDetail.positionName / UserRoleDetail.userName 一致；列表 + 详情都能直接展示。

**N+1 trade-off**：列表场景每行查 User — 与 v0.0.7 UserRole.enrich KL-4 同款，接受为 v0 admin 场景。

### 8. 前端 owner 默认选中 — listUsers + loginName 匹配

**方案**：`ProjectsPage` 编辑抽屉打开时：
```ts
void listUsers({ size: 100 }).then(r => {
  setUsers(r.content);
  if (!editing) {
    const currentLogin = useAuthStore.getState().user?.username;
    const self = r.content.find(u => u.loginName === currentLogin);
    setOwnerUserId(self?.id ?? '');
  } else {
    setOwnerUserId(editing.ownerUserId);
  }
});
```

**为什么**：
- 不引入 GET /api/auth/me 增强（保持 v0 鉴权 API 最小）
- 默认选中 = current user 体现"创建者通常就是 owner"的常见路径
- 用户可改成任意其他 user（这是 owner 可改的 UI 落地）
- 编辑时回显 editing.ownerUserId 且不 disabled（可改）

**边界情况**：当前登录用户没 User 行（如登录 "admin" 但 DB 无 loginName=admin）→ self 为 null → 默认空 → admin 手动选

### 9. Sider 菜单插入位置 + 路由

**方案**：
- Sider「需求管理」组追加「项目」一项，**插在「诉求」之前**（项目是上位概念）
- 路由 `/pm/projects`
- 不引入新菜单组（项目仍是 PM 范畴）

**为什么**：项目 → 需求 → 诉求 的业务包含关系；菜单按业务层级排序。

### 10. 数据策略 — 不 down -v；ddl-auto=update 增表

**方案**：
- 交付时只 `RAINIER_BACKEND_HOST_PORT=18080 docker compose build backend frontend && docker compose up -d --no-deps --force-recreate backend frontend`
- Hibernate `ddl-auto=update` 自动 `CREATE TABLE rainier_project (...)`
- 现有 9 张表的列、数据全保留
- mysql 卷不动

**为什么**：
- v0.0.4-0.0.7 都用 down -v 是因为有 schema 改动（drop column / add unique constraint 等），但 ddl-auto 不删列；本次纯增表，不需要清卷
- 保留现有手测数据让用户直接验证富化效果

**风险**：如果未来某次实现引入了不兼容的 schema 改动，必须回退到 down -v 路径，并在 design-adjustments 记录。

## Architecture

```
┌─ 写入链路 ──────────────────────────────────────────────────────────┐
│                                                                      │
│  POST /api/projects ─▶ ProjectController ─▶ ProjectService.create   │
│      body 含 ownerUserId               · userRepo.existsById         │
│                                          (不存在 → 400)              │
│                                        · code 唯一校验               │
│                                        · status 集合校验             │
│                                        · saveAndFlush                │
│                                        · enrich → 返回带 ownerName    │
│                                                                       │
│  PUT /api/projects/{id} ─▶ Controller ─▶ Service.update             │
│      body 含 ownerUserId (可改)         · 若 != 原值 → 校验存在       │
│                                        · 其他字段照常 update         │
│                                                                       │
│  POST /api/requirements (含 projectId) ─▶ RequirementService.create │
│                                            · 若 projectId != null →  │
│                                              projectRepo.existsById  │
│                                              (不存在 → 400)          │
│                                            · 后续逻辑同 v0.0.6       │
│                                            · enrich 加 projectName / │
│                                              projectCode 富化         │
│                                                                       │
│  POST /api/user-roles (含 projectId) ─▶ UserRoleService.create      │
│                                          · 若 projectId != null →    │
│                                            projectRepo.existsById    │
│                                          · 若 null → 跳过 (公司级)    │
│                                          · enrich 加 projectName     │
└──────────────────────────────────────────────────────────────────────┘

┌─ 读取链路（富化 + 容错） ─────────────────────────────────────────┐
│                                                                      │
│  GET /api/projects/{id} ─▶ Service.findById                          │
│                              · userRepo.findById(ownerUserId) →      │
│                                set ownerName + ownerLoginName        │
│                              · 容错：找不到 → 留 null (不 throw)      │
│                                                                       │
│  GET /api/requirements/{id} ─▶ RequirementService.enrich             │
│                                  · projectRepo.findById(projectId) → │
│                                    set projectName + projectCode     │
│                                  · 容错：projectId=42 (脏) → null    │
│                                                                       │
│  GET /api/user-roles ─▶ UserRoleService.list                         │
│                          · 每行 enrich：userName/userLoginName +     │
│                            roleName/roleCode +                       │
│                            projectName/projectCode (新增)             │
│                          · 容错：脏 projectId 返 null projectName    │
└──────────────────────────────────────────────────────────────────────┘

┌─ 删除保护链路 ────────────────────────────────────────────────────┐
│                                                                      │
│  DELETE /api/projects/{id} ─▶ Service.delete                         │
│                                · requirementRepo.countByProjectId   │
│                                  > 0 → 409 "linked requirements"     │
│                                · userRoleRepo.countByProjectId       │
│                                  > 0 → 409 "assigned user-roles"     │
│                                · = 0 → softDelete (@SQLDelete)       │
└──────────────────────────────────────────────────────────────────────┘

┌─ 前端 ──────────────────────────────────────────────────────────────┐
│                                                                      │
│  AppLayout (Sider)「需求管理」组                                     │
│       ├─ 项目         ─▶ /pm/projects   ProjectsPage (NEW)           │
│       ├─ 诉求         ─▶ /pm/demands    (v0.0.6)                     │
│       ├─ 需求         ─▶ /pm/requirements                            │
│       └─ 诉求-需求关联 ─▶ /pm/demand-requirements                    │
│                                                                      │
│  ProjectsPage 编辑抽屉:                                              │
│       · 主表单（code/name/description/status/dates/enabled）         │
│       · 「负责人」下拉 (异步 listUsers, 默认 = 当前登录 user)        │
│       · 编辑时回显 ownerUserId，**不 disabled**，可改                 │
│                                                                      │
│  RequirementEditDrawer (改造):                                       │
│       · projectId 控件: 数字输入框 → Project 下拉 (异步 listProjects)│
│       · 编辑时回显                                                    │
│                                                                      │
│  UserRolesPage (改造):                                               │
│       · projectId 控件: 数字输入框 → Project 下拉 + 留白选项         │
│       · 留白 = 公司级 hat (传 null)                                  │
│                                                                      │
│  RequirementsPage / UserRolesPage 列表 + 项目 列                     │
│       · render: projectName ? `${projectName} (${projectCode})` : '—'│
└──────────────────────────────────────────────────────────────────────┘
```

## Risks / Trade-offs

| 风险 | 缓解措施 |
|---|---|
| ProjectsPage 默认 owner 解析依赖 listUsers 拉池，size: 100 上限 — 如果用户数 > 100 默认会错 | v0 用户数远低于 100；后续 admin 加超过 100 时可加搜索分页或专门的 GET /api/users/me；test-report 记入 KL |
| 现有脏数据 `user_role.id=2 projectId=42` 富化返 null — UI 显示"—" | 用户在手测确认时已知；admin 可以手动编辑该行改对或删除；不强制清理 |
| owner 可改 → admin 误操作把 owner 改成不相干人 — 无 audit trail 记录 owner 变更历史 | v0 接受；JPA auditing 已记录 `updateBy` 和 `updateTime`；要更细要单独建 audit log |
| Project.delete 时 Requirement 和 UserRole 都有 FK 引用 — 错误顺序可能让 admin 反复挫败 | 错误 message 明确指出"是 Requirement 还是 UserRole 在用"；admin 按指引逐个清理 |
| Hibernate ddl-auto=update 增表时 owner_user_id FK 不会自动建（ddl-auto 不强制 FK） | service 层 existsById 是唯一真相源（v0.0.6 已建立此模式）；DB FK 缺失不影响 application 行为 |
| RequirementEditDrawer 改造可能破坏 v0.0.6 既有 vitest（TC-FES-D03） | TC-FES-D03 mock 的 createRequirement 还是收到 sourceDemandIds — projectId 控件改造不影响该 mock 的契约；执行时 vitest 应仍绿 |
| Frontend 容器单独 rebuild 时 backend image 需保持 — `docker compose up --no-deps --force-recreate` 是关键 | 沿用 v0.0.7 手测修复时的同款命令 |
| Phase 5 review 可能命中 1-2 项 H（参考 v0.0.7 命中 1 H 的体量） | 长程模式预留迭代次数；review-driven 修复或接受 KL |
