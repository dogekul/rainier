# v0.0.16-project-type — 技术设计

## Context

- 栈: Java 8 / Spring Boot 2.7 / JPA(Hibernate) / MySQL(dev+docker, `ddl-auto=update`) / H2(test, `create-drop`)。
- `Project`(`com.rainier.project.domain.Project`) 现有 `code/name/description/status/ownerUserId/startDate/endDate/enabled`，soft-delete(`@SQLDelete`+`@Where del_flag=0`)，`code` service 级唯一。
- `ProjectStatus` 是常量类(非 enum): `PLANNING/ACTIVE/ON_HOLD/DELIVERED/ARCHIVED` + `ALL = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(...)))`。`ProjectService` 用 `ALL.contains(...)` 校验，create 默认 `PLANNING`。
- 既有 bootstrap 自愈 runner `DanglingProjectIdCleanup`(`CommandLineRunner`+`@Order(HIGHEST_PRECEDENCE)`+native SQL)。
- 前端 `ProjectsPage` 内联 Drawer(非独立 EditDrawer)，`STATUS_OPTIONS` 数组 + `<select data-testid="projects-status-select">`，表格列硬编码。当前页**无** per-field 过滤 UI(仅 search)。
- v0.0.15 `AuditAspect` 已对 `*Service.update` 自动记审计 → 转化(update)留痕白拿，本版不新做。
- 约束: A2 收窄——**无审批、无完整性校验门**;standing——不删改存量业务数据。

## Decisions

### 1. 字段名 `projectType` + 通用可扩展枚举(非 scale 二值)

**方案**: `Project` 加 `projectType` 列(`@Column(name="project_type", length=16)`，**nullable**)。值域初始 `CASUAL`/`FORMAL`，但语义上是「项目类型」通用枚举，未来可加值作他用。

**为什么**: 用户明确要通用可扩展字段而非 scale 二值，命名 `projectType` 给将来留空间(如加 `RESEARCH`/`OPS` 等)。

**备选及排除**: (A) `scale BOOLEAN`/`isFormal` —— 不可扩展，排除。(B) Java `enum ProjectType` —— 加值需改代码+DB 枚举迁移，不如常量类+VARCHAR 灵活;且与既有 `ProjectStatus`(常量类)不一致，排除。

### 2. `ProjectType` 常量类照搬 `ProjectStatus`

**方案**: 新 `com.rainier.project.domain.ProjectType`: `public static final String CASUAL="CASUAL"; FORMAL="FORMAL";` + `ALL = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(CASUAL, FORMAL)))` + private 构造。

**为什么**: 与 `ProjectStatus` 同款，团队已熟悉;Java 8 无 `Set.of`，用 `Arrays.asList`。

### 3. create: 省略 → 默认 `CASUAL` + 枚举合法性校验

**方案**: `ProjectService.create` 镜像 status: `String type = req.getProjectType()==null ? ProjectType.CASUAL : req.getProjectType();` 然后 `if(!ProjectType.ALL.contains(type)) throw new BadRequestException("invalid project type: "+type);` `p.setProjectType(type)`。

**为什么**: D3 决策——create 可省;与 status 默认 `PLANNING` 一致。

### 4. update: present+valid→set / present+invalid→400 / **absent→保留原值**

**方案**: `ProjectService.update`: `if(req.getProjectType()!=null){ if(!ProjectType.ALL.contains(...)) throw 400; p.setProjectType(...); }` —— null 时**不动**，保留 `p` 现值。

**为什么**: 与 status(update 必填、全量替换)**故意不同**。若 update 省略 projectType 就清空/默认 CASUAL，会把一个 `FORMAL` 项目**静默降级**——违反 standing(不改存量分类)。保留语义最安全;前端下拉总会送值，保留分支只是防御。

**备选及排除**: (A) update 必填(@NotBlank)同 status —— 老 payload/部分更新会 400 或静默降级，排除。

### 5. 「转化」= 普通 update，无专用端点/审批/校验门

**方案**: 轻量→正式 就是 `PUT /api/projects/{id}` 把 projectType 改 `FORMAL`。不加 `promoteToFormal` 端点、不加审批字段(approvedBy)、不加「正式内容补齐」校验。

**为什么**: A2 收窄——无审批、暂不校验。转化与「直接创建正式项目」等价。仅保留枚举合法性校验。

### 6. list 过滤: Specification + Controller param

**方案**: `ProjectService.list` 增 `projectType` 入参，Specification 加 `if(projectType!=null) p=cb.and(p, cb.equal(root.get("projectType"), projectType))`;`ProjectController.list` 加 `@RequestParam(required=false) String projectType`。

**为什么**: 照搬既有 status filter 模式，最小 touch。

### 7. 存量回填: `ProjectTypeBackfill` runner + 读路径 null→CASUAL 双保险

**方案**: 新 `com.rainier.project.bootstrap.ProjectTypeBackfill`(`CommandLineRunner`+`@Order(HIGHEST_PRECEDENCE)`+`@Transactional`+native `UPDATE rainier_project SET project_type='CASUAL' WHERE project_type IS NULL`，log 影响行数)。**同时** `ProjectDetail.from` 兜底 `dto.projectType = p.getProjectType()==null ? ProjectType.CASUAL : p.getProjectType()`。

**为什么**: D2——DB 列 nullable(避开 MySQL strict 模式 `NOT NULL` ALTER 在存量行上失败);回填把存量补成 CASUAL;读兜底保证即使回填未跑/并发窗口也不返 null。双保险。

**备选及排除**: (A) `@Column(columnDefinition="varchar(16) not null default 'CASUAL'")` —— DB 方言耦合、绕过 JPA 类型，排除。(B) 仅读兜底不回填 —— DB 里仍是 null，`?projectType=CASUAL` 过滤漏掉存量行，排除。

### 8. 前端: 下拉镜像 status + 中文标签 + 类型列 + 类型过滤

**方案**:
- `api/project.ts`: `ProjectType = 'CASUAL'|'FORMAL'`;`Project`/`ProjectCreate`/`ProjectUpdate`/`ProjectListParams` 加 `projectType`。
- `ProjectsPage`: `PROJECT_TYPE_OPTIONS: ProjectType[]=['CASUAL','FORMAL']` + 标签 map `{CASUAL:'轻量',FORMAL:'正式'}`;Drawer 加 `<select data-testid="projects-type-select">`(默认 `CASUAL`，编辑时取 `editing.projectType`);表格加「类型」列(render 中文标签);表格上方加类型过滤 `<select data-testid="projects-type-filter">`(含「全部」)，扩展 fetcher 把 typeFilter 并入 `listProjects`。

**为什么**: 镜像既有 status select 模式;中文标签提升可读性(status 列仍英文，类型列用中文);过滤 UI 是本页新增(本页原无 per-field 过滤)，符合 proposal 的「过滤」范围。

## Architecture

```
POST/PUT /api/projects ──► ProjectController ──► ProjectService
                                                   ├─ create: type = req ?? CASUAL; assert ∈ ALL; set
                                                   ├─ update: req!=null ? (assert ∈ ALL; set) : 保留
                                                   └─ list(projectType): Spec equal predicate
                              GET ──► ProjectDetail.from(p): projectType = p.type ?? CASUAL  (读兜底)

启动 ──► ProjectTypeBackfill(@Order HIGHEST) ──► UPDATE rainier_project SET project_type='CASUAL' WHERE IS NULL

update ──► (v0.0.15) AuditAspect @AfterReturning ──► rainier_audit_log: UPDATE PROJECT#id  (白拿)

前端 ProjectsPage: [类型过滤 select] → fetcher(projectType) → listProjects
                   表格[..., 类型列(中文)] / Drawer[..., 类型 select(默认轻量)]
```

## Risks / Trade-offs

| 风险 | 缓解措施 |
|------|----------|
| 既有「GET 详情返完整字段」scenario 与其测试硬编码字段集 exact-equality，加 projectType 会破断言 | Build 阶段**同步**更新该 spec scenario 字段集 + 对应测试断言(加 `projectType`);列为 PA 记录 |
| H2 test `create-drop` 无存量行 → 回填 runner 自然 no-op，回填逻辑无覆盖 | 回填测试**手动注入** project_type=NULL 行(native `UPDATE ... SET project_type=NULL WHERE id=?` 或直接 native INSERT)后调 runner / 重启上下文验证 → CASUAL |
| update 保留语义与 status required 不一致，易误读 | design Decision 4 + spec scenario 显式写「absent→保留」;javadoc 注明防静默降级 |
| MySQL strict 模式对存量行加 `NOT NULL` 列 ALTER 失败 | 列设 **nullable**(D2);回填 + 读兜底补默认 |
| 前端 fetcher 扩展 typeFilter 引入闭包 stale 风险 | typeFilter 入 `useCallback` deps，change 后 `list.refetch()` 或 setPage(0) 触发重查 |
| 契约不一致(后端 projectType vs 前端读取) | TC-FES + E2E 交叉验证字段名;list param 名一致 `projectType` |
