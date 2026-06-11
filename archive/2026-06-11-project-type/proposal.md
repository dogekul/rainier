# v0.0.16-project-type — Project 可扩展「项目类型」字段

> Baseline: tag `v0.0.15-audit-log` / commit e6b0878. 当前 backend 309 + frontend 58 测试 green, 18 张表.
> 来源: 角色卡 `A-角色意图卡片.md` §2.3 (项目规模分级 + 轻量→正式转化), gap 项 A2 收窄版.

## Why

角色卡 §2.3 要求项目分「轻量/正式」两档(规模分级 + 轻量→正式转化)。当前 `Project` 实体只有
`code/name/description/status/ownerUserId/startDate/endDate/enabled`，**没有任何类型/规模维度**，
无法表达这个区分。本版加一个**通用可扩展的 `projectType` 字段**承载它——命名刻意通用(不叫 scale)，
让未来枚举值可扩展作他用。按用户锁定的收窄: **无审批、无完整性校验**——「轻量→正式 的转化」就是把
字段从 `CASUAL` 改成 `FORMAL` 的一次普通 update，与直接创建一个正式项目完全一样。

## What Changes

- `Project` 新增 `projectType` 列(可扩展枚举，初始 `CASUAL`/`FORMAL`，默认 `CASUAL`)。
- 新增 `ProjectType` 常量类(`CASUAL`/`FORMAL` + `ALL` set，照搬 `ProjectStatus` 模式，可扩展)。
- `ProjectCreateRequest` / `ProjectUpdateRequest` / `ProjectDetail` 透传 `projectType`;create 省略则默认
  `CASUAL`，update 全量替换(同 status 语义)。
- `ProjectService` 校验 `projectType ∈ ProjectType.ALL`(**仅枚举合法性**，无「正式内容补齐」业务门 —— 暂不校验)。
- `ProjectController` list 端点加 `projectType` 过滤参数;`ProjectService.list` 加 projectType filter。
- 存量 project(无类型)→ 启动时一次性 bootstrap 回填 `CASUAL`(照搬 `DanglingProjectIdCleanup` runner 模式)，
  读路径 null→CASUAL 兜底。
- 前端 `api/project.ts` 加 `projectType` 类型;`ProjectsPage` 新建/编辑 Drawer 加「项目类型」下拉
  (`PROJECT_TYPE_OPTIONS`，镜像既有 `projects-status-select`)+ 表格加「类型」列 + 过滤。

## Capabilities

### Modified Capabilities

- `entity-project`: Project 实体加 `projectType` 维度(create/update/list/detail 全链透传 + 枚举校验 + 存量回填)。
- `frontend-scaffold`: ProjectsPage 编辑抽屉加类型下拉 + 表格类型列 + 过滤(无新增页/路由/Sider 组)。

### New Capabilities

- 无(纯实体加字段，不新增 capability、不新增表)。

## Impact

**代码层面**(均为既有文件，无新包):
- 后端:`Project.java`(+1 列) / `ProjectType.java`(新常量类) / `ProjectCreateRequest` / `ProjectUpdateRequest` /
  `ProjectDetail`(3 DTO 透传) / `ProjectService`(create + update + list filter + 读兜底) /
  `ProjectController`(list 加 `projectType` param) / 新 1 个 bootstrap 回填 runner(`ProjectTypeBackfill`，project.bootstrap 包)。
- 前端:`api/project.ts`(+projectType 类型 + ProjectListParams) / `ProjectsPage.tsx`(+类型下拉 +类型列 +过滤) /
  `ProjectsPage.test.tsx`(新增用例)。
- 测试:service / controller 层加 projectType 用例 + 前端 Drawer/列 用例 + E2E 存量回填验证。

**配置层面**: 无(`ddl-auto=update` 自动加列;H2 test `create-drop` 无存量)。

**基础设施**: **0 新表**(仅 `rainier_project` 加 1 列);无新依赖、无新服务、无新端点(仅 list 加 query param)。

## 已锁定决策 (Gate 1 确认 2026-06-11)

- **D1 枚举常量名**: `CASUAL` / `FORMAL`(前端显示「轻量」/「正式」)。
- **D2 列约束 + 存量回填**: DB 列 **nullable** + 启动时 bootstrap 一次性回填 `CASUAL` + 读路径 null→CASUAL 兜底
  (对现有 MySQL 数据最安全，避开 strict 模式 `NOT NULL` ALTER 失败)。
- **D3 create 时 projectType**: **可省**(省则默认 `CASUAL`，同 status 的 `PLANNING` 默认)。

## 显式排除 (推后续版本)

- 正式项目的「补齐内容」字段(风险评估 / 范围声明)—— 本版不加。
- 「里程碑」= gap 项 B4(未建)。
- 完整性校验门(正式内容补齐才能转)/ 团队负责人审批工作流 / 流程·AI·审计强度差异化。
- projectType 的权限收口(谁能创建正式项目)—— 角色卡 §2.3 述「团队负责人及以上」，本版不做。

## Success Criteria

- [ ] `POST /api/projects` 省略 projectType → 持久化为 `CASUAL`;显式传 `FORMAL` → 持久化 `FORMAL`。
- [ ] `PUT /api/projects/{id}` 把 `CASUAL` 改 `FORMAL` 成功(转化 = 普通 update，无审批 / 无完整性门)。
- [ ] 非法 projectType(如 `"XXX"`)→ 400 BadRequest。
- [ ] `GET /api/projects?projectType=FORMAL` 仅返回正式项目。
- [ ] `ProjectDetail` 响应含 `projectType` 字段。
- [ ] 存量 project(docker MySQL 现有数据)启动后 projectType 全为 `CASUAL`，**其余字段一字未改**(standing 约束)。
- [ ] 前端编辑抽屉有「项目类型」下拉，表格有「类型」列，可按类型过滤。
- [ ] backend 测试全绿(309 baseline + 新增);frontend 全绿(58 baseline + 新增)+ tsc clean;E2E `SHOW TABLES`=18 不变。
