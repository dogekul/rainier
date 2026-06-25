# Design — Project Team / PMO / Members (v0.0.64)

## Context

### 现状
- `rainier_project` schema 已有 `organization_id BIGINT NULL` 列（v0.0.49 起就有），但**前端从未启用**：ProjectEditDrawer 无团队选择控件；ProjectDetail 无团队展示
- `rainier_organization` 是树（parent_id + path），但**没有指针**指向 "本 org 的 PMO"
- `rainier_role` 已有 PMO/YFM 两个 role；`rainier_user_role` 已支持 project 维度（user_role.project_id）但**不支持 organization 维度**
- 项目 owner 是 single user_id；缺第二位关键角色（PMO）字段
- 没有"项目成员"概念实体；勉强可用 user_role+project_id 凑，但 user_role 承载权限角色语义，把"团队某 DEV 加入项目"挤进去概念不正

### 约束
- 后端 Java 8 + Spring Boot 2.7（Docker temurin-8 是真闸；本地 jdk 25 lenient）
- ddl-auto=update 自动建新表，无 migration 脚本
- AuditAspect 切面已就位（自动捕获 *Service.create/update/delete）—— 新 service 命名遵循 record/query 防自审计
- BaseEntity 提供 createBy/createTime/updateBy/updateTime/delFlag；新表全部 extends
- 现有 26 张表 → 加 2 张 → 28（`LegacyProductCategoryCleanupTest` 表数断言要同步改）

## Decisions

### D1 — PMO 与 Organization 用 M:N 单独表，不复用 user_role
**方案**：新表 `rainier_organization_pmo (id, organization_id, user_id, BaseEntity)`，UNIQUE(organization_id, user_id, del_flag)

**为什么**：
- user_role 的语义是"权限角色"（PMO/YFM 等可决定能做什么）；这里我们要的是"这个 user 是这个 org 的 PMO 之一"的归属关系；混在一起会让 user_role 既要承载 project 维度也要承载 org 维度，语义混乱
- 1 row = 1 association，CRUD 直观

**排除方案**：
- ❌ `organization.pmo_user_id BIGINT NULL` 单列：违反 1:N（用户已确认）
- ❌ 在 user_role 上 +organization_id 列：可行但拖垮 user_role 语义；以后想给 org 维度加别的 role 时还得再扩展

### D2 — PMO 继承采用读时计算，不冗余写入下级
**方案**：`OrganizationPmoService.findEffectivePmos(orgId)`
1. `OrganizationService.getAncestorIds(orgId)` 沿 parent_id 链向上回溯，返回 [self, parent, grandparent, ...]
2. `organizationPmoRepo.findByOrganizationIdIn(ancestorIds)` 一次查
3. enrich：每条带 `inheritedFromOrgId / inheritedFromOrgName`；自身 PMO 对应 inheritedFromOrgId = orgId

**为什么**：
- 写时传播（在子 org 复制行）需要处理：父 add → 子全 add；父 delete → 子哪些 delete（要区分 own vs inherited）；子 add 同一 user 与父冲突；运维复杂
- 读时计算：org_pmo 表始终是 own 数据；任何变更只动一行；inheritance 由 service 层计算
- 性能：org 树通常 <10 层，ancestor 查询 O(深度) 次 parent_id 跳转 + 1 次 IN 查询；远低于树写入级联

**排除方案**：
- ❌ 写时复制（materialize）：删除/更新级联复杂
- ❌ 仅查询自身（不继承）：和用户「上级配 PMO → 下级自动配上」明确冲突
- ❌ 用 path 列 LIKE 查祖先：path 在 org 树仅是辅助，不一定及时维护；parent_id 链更可靠

### D3 — Project 默认值（organizationId / pmoUserId）后端注入
**方案**：`ProjectService.create(req)` 流程：
```
if (req.organizationId == null) {
  req.organizationId = userOrganizationRepo
    .findByUserIdAndIsPrimaryTrueAndLeftAtIsNull(req.ownerUserId)
    .map(UserOrganization::getOrganizationId)
    .orElse(null);   // owner 无主组织 → 团队字段空
}
if (req.pmoUserId == null && req.organizationId != null) {
  req.pmoUserId = organizationPmoService
    .findEffectivePmos(req.organizationId)
    .stream().findFirst().map(EffectivePmo::getUserId).orElse(null);
}
```

**为什么**：
- 单一真理：前端可显示「建议默认」，但最终值由后端定，避免前端拿到 cached user 数据导致漂移
- 与既有「创建项目时 owner 不传则用当前登录用户」模式一致
- 前端 UX：form 打开时**异步预填**（query owner 主组织 + effective-pmos），让用户看到默认值；用户改动后正常 POST；后端 if-null 注入保护防止前端 bug

**排除方案**：
- ❌ 前端纯填：依赖前端实现一致性；不同入口（如未来 API 调用）默认丢失

### D4 — 项目内角色枚举 7 项，存 VARCHAR(16)
**方案**：`project_member.role VARCHAR(16) NOT NULL` 必填字段；后端 `ProjectMemberRole` 常量类
```java
public final class ProjectMemberRole {
  public static final String PD = "PD";          // 产品经理
  public static final String DEV = "DEV";        // 研发
  public static final String QA = "QA";          // 测试
  public static final String DESIGN = "DESIGN";  // 设计
  public static final String BIZ = "BIZ";        // 业务
  public static final String OPS = "OPS";        // 运维
  public static final String OTHER = "OTHER";    // 其他
  public static final java.util.Set<String> ALL = new java.util.HashSet<>(java.util.Arrays.asList(PD,DEV,QA,DESIGN,BIZ,OPS,OTHER));
}
```
前端 `PROJECT_MEMBER_ROLE_LABELS` map 出中文。

**为什么**：
- 7 项覆盖大多数 IT 项目角色（无 PM/项目经理：那是 owner 的语义）
- 字符串 enum 比 int 易读、易扩展、JSON 自然
- 字典表（rainier_role_dict 之类）overkill：7 项稳定不变；前端硬编码即可

**排除方案**：
- ❌ FK 到 rainier_role：rainier_role 是权限角色；项目内角色是描述性身份，不挂权限
- ❌ enum 类型存 INT：可读性差；DB 迁移痛苦

### D5 — owner 不冗余进 project_member，read 时 UNION 合成行
**方案**：`ProjectMemberService.listMembers(projectId)` 返回 `List<ProjectMemberDetail>`，顺序：
1. owner 合成行 role="OWNER" displayLabel="负责人"（不可删，UI 不显示移除按钮）
2. pmo 合成行（若 pmo_user_id 非空且 != ownerUserId）role="PMO" displayLabel="项目PMO"（同上）
3. 真实 project_member 行 按 joined_at DESC（实际 role）

**为什么**：
- owner 数据已在 `project.owner_user_id`；写到 project_member 会两份数据要同步（owner 改了得 update member 表）
- pmo 同理
- 合成行让 UI 不用区分数据来源；UI 看到统一的 List<Member> 即可

**排除方案**：
- ❌ Owner 写入 project_member（role=OWNER）：同步成本高；owner 变更要 update 该行；删除 member 要防误删
- ❌ Owner 不显示在成员列表（仅 project_member 数据）：UX 烂；用户期望看到完整团队列表

### D6 — 权限双重防御：前端隐藏 + 后端 403
**方案**：
- 后端：`ProjectMemberController.add/update/delete` 进入前调 `authz.canManageProjectMembers(currentUser, projectId)`：当前用户 == project.ownerUserId OR == project.pmoUserId OR currentUser.adminAccess → 通过；否则 throw `ForbiddenException` → 403
- 前端：`ProjectDetailPage.MemberTab` 接收 `currentUserId/currentUserAdminAccess` props；按相同规则决定「添加成员」「移除」「角色 select」是否渲染

**为什么**：
- 后端是真权限；前端是 UX（不让用户看到禁用按钮）
- 双层防止：第三方调用 API 也阻止（不能仅靠前端隐藏）

### D7 — TreeSelect 异步联动：team 切换 → pmo 候选重列
**方案**：
- `ProjectEditDrawer` 增加 state：`teamOrgId`, `pmoUserId`, `pmoCandidates: EffectivePmo[]`, `pmoLoading: bool`
- `useEffect([teamOrgId])`：teamOrgId 变 → 设 pmoLoading=true，调 `getEffectivePmos(teamOrgId)`；返回后 setPmoCandidates(res)，setPmoUserId(res[0]?.userId ?? '')，setPmoLoading=false
- pmo `<select>` disabled when pmoLoading；选项里继承的 PMO 后缀 `（继承自 XX）`

**为什么**：
- 给用户即时反馈：换团队后 PMO 候选立即对应
- 自动重选默认避免 stale pmoUserId

**陷阱**：
- 旧 selected pmoUserId 必须清空 / 重置；否则可能 POST 到一个不属于新 team 的 user
- 切换中 form save 必须 disabled

### D8 — 后端响应 enrich：所有 List/Get 返回带名字
**方案**：
- `ProjectDetail` 增加 `pmoUserId / pmoName / pmoLoginName / organizationId / organizationName / organizationType`
- `ProjectMemberDetail` 含 `userId / userName / userLoginName / role / roleLabel / joinedAt / joinedBy`（合成 owner 行的 joinedAt 取 project.createTime）
- `EffectivePmoDetail` 含 `userId / userName / userLoginName / inheritedFromOrgId / inheritedFromOrgName`
- 后端 service 完成 join，前端不用单独查 user

**为什么**：
- 前端"永不裸露 ID" 规则（v0.0.60 立的）
- 减少前端 N+1 查询

### D9 — Java 8 兼容
- 不用 `Set.of() / List.of()` → 用 `new HashSet<>(Arrays.asList(...))` / `Collections.unmodifiableList(Arrays.asList(...))`
- 不用 `var` → 显式声明类型
- 不用 `Optional.orElseThrow()`（无参版本是 Java 10+）→ 显式 `.orElseThrow(() -> new NotFoundException("..."))`

### D10 — Seed-demo.sql 更新
- 给 6 个 organization 配 own PMO：根 招联金融 (id=1) → alice; 研发中心 (id=2) → 黎立; 子团队基本不 own 配（演示继承）
- 给 6 个 project 配 organizationId + pmoUserId（取所属 org 的 effective-PMO）
- 每个 EXTERNAL_DELIVERY 项目（ED-1/2/3）加 3-5 个 project_member 跨 role
- 内部项目（CF-4/CT-5/CAS-6）加 1-2 个 project_member

## Architecture

### 数据流：创建项目

```
[Frontend ProjectEditDrawer]
  drawer 打开
    → GET /api/users/me/primary-org-id          (查 owner 主组织默认值)
    → GET /api/organizations/{org}/effective-pmos (查 team 的 PMO 候选)
  user 提交
    → POST /api/projects  { name, ownerUserId, organizationId, pmoUserId, ... }

[Backend ProjectController.create]
  → ProjectService.create(req)
    if (req.organizationId == null) inject from user_organization (is_primary=1)
    if (req.pmoUserId == null && organizationId != null)
      inject from organizationPmoService.findEffectivePmos(organizationId)[0]
    validate + persist
    return ProjectDetail (enriched with org / pmo names)
```

### 数据流：成员管理

```
[Frontend ProjectDetailPage 成员 Tab]
  挂载 → GET /api/projects/{id}/members
    response: owner-合成行 + pmo-合成行(若不同) + project_member 真实行 (各带 enriched names)
  添加 按钮（仅 canManage 时渲染）→ 弹窗 (user select + role select) → POST /api/projects/{id}/members
  改 role → PUT /api/projects/{id}/members/{userId} body {role}
  移除 → DELETE /api/projects/{id}/members/{userId}

[Backend ProjectMemberController]
  add/update/delete:
    → authz.canManageProjectMembers(currentUser, projectId)
       must be owner OR project.pmo OR currentUser.adminAccess
       否则 throw ForbiddenException (→ 403)
    → add 时检查 userId != project.ownerUserId 否则 400「该用户已是项目负责人」
    → add 时检查 UNIQUE(project, user, del_flag=0) 否则 409
    → delete 时检查 userId != ownerUserId 否则 400
```

### 数据流：PMO 继承查询

```
[Frontend ProjectEditDrawer]
  team selector 变 → GET /api/organizations/{X}/effective-pmos
    response: [
      { userId: 5, userName: 黎立, userLoginName: lili, inheritedFromOrgId: 2, inheritedFromOrgName: 研发中心 },
      { userId: 1, userName: Alice, userLoginName: alice, inheritedFromOrgId: 1, inheritedFromOrgName: 招联金融 }
    ]
  下拉显示「黎立 (lili)（继承自 研发中心）」等

[Backend OrganizationPmoService.findEffectivePmos(orgId)]
  ancestorIds = organizationService.getAncestorIds(orgId)
    // 例如 orgId=6（采购研发团队），ancestor chain 通过 parent_id：[6, 2, 1]
  rows = organizationPmoRepo.findByOrganizationIdInAndDelFlagFalse(ancestorIds)
  // enrich：join user, join organization → 设 inheritedFromOrgId/Name
  return rows (各带源 org 信息)
```

### 组件图

```
backend/
  organization/
    domain/Organization.java      (existing)
    service/OrganizationService.java  (+getAncestorIds)
  organizationpmo/   (NEW package)
    domain/OrganizationPmo.java
    dto/OrganizationPmoDetail.java
    dto/OrganizationPmoCreateRequest.java
    dto/EffectivePmoDetail.java
    repository/OrganizationPmoRepository.java
    service/OrganizationPmoService.java
    controller/OrganizationPmoController.java
  project/
    domain/Project.java           (+pmoUserId)
    dto/ProjectCreateRequest.java (+organizationId/pmoUserId nullable)
    dto/ProjectUpdateRequest.java (+organizationId/pmoUserId)
    dto/ProjectDetail.java        (+pmoUserId/pmoName/orgName/orgType/orgId)
    service/ProjectService.java   (+default-injection)
  projectmember/   (NEW package)
    domain/ProjectMember.java
    domain/ProjectMemberRole.java (constants)
    dto/ProjectMemberDetail.java
    dto/ProjectMemberCreateRequest.java
    dto/ProjectMemberUpdateRequest.java
    repository/ProjectMemberRepository.java
    service/ProjectMemberService.java
    controller/ProjectMemberController.java
  me/
    service/MeService.java        (+UNION project_member into listMyProjects)
  authz/
    service/AuthzService.java     (+canManageProjectMembers)

frontend/src/
  api/
    organizationPmo.ts        (NEW)
    projectMember.ts          (NEW)
    project.ts                (+organizationId/pmoUserId fields)
  constants/labels.ts         (+PROJECT_MEMBER_ROLE_LABELS)
  pages/
    Project/
      ProjectEditDrawer.tsx   (+team TreeSelect + pmo select + 联动)
      ProjectDetailPage.tsx   (+Hero chips + 成员 Tab + 添加弹窗)
      ProjectsPage.tsx        (+team column)
    Organization/
      EditDrawer.tsx          (+PMO 管理段)
```

## Risks / Trade-offs

| 风险 | 概率 | 影响 | 缓解 |
|---|---|---|---|
| TreeSelect 异步联动竞态（用户快速切换 team） | 中 | 中 | 用 AbortController + lastRequestId 标记；只接受最新请求结果 |
| owner 既是 team 主组 但 effective-PMOs 为空 | 高 | 低 | 后端 if-null 兜底，PMO 字段允许 NULL，前端显示 "—" |
| 父 org 删 PMO → 子 org 项目继续引用该 user（detach） | 中 | 低 | project.pmo_user_id 是冻结值；删 org_pmo 不影响 project（已显式说明 D3） |
| inherited PMO 在子 org delete 按钮误点 | 中 | 低 | UI 上 inherited chip 灰禁用 + 后端 400 兜底 |
| 同一 user 是 owner 且被加成员 → UNIQUE 没冲突但展示重复 | 低 | 中 | 后端 add 时优先校验 owner 比对（D6 流程已写） |
| Owner 改 → 旧 owner 应处理？ | 低 | 中 | 已锁定：不自动入 member 表；UI 上 owner 改字段时给个 toast 提醒 |
| project_member.role 列加 VARCHAR(16) 后 service 验证遗漏 | 低 | 中 | 创建/更新 service 校验 role ∈ ALL；否则 400 |
| 表数 26 → 28 后 LegacyProductCategoryCleanupTest 断言挂 | 高（必发生） | 低 | 同步改测试断言 |
| Java 8 误用 Set.of/List.of/var | 中 | 高（Docker 构建失败） | Code 风格 checklist；review 时 grep `Set.of\|List.of\| var ` |
| AuditAspect 对新 service 是否生效 | 低 | 低 | service 命名遵循 createXxx/updateXxx/deleteXxx → AspectJ pointcut 自动匹配 |
| 前端 owner 变更后 ProjectEditDrawer pmo 候选可能 stale | 中 | 低 | owner select onChange → 触发 team-select 默认重算 → 触发 pmo 重列 |
