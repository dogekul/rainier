# Test Plan: v0.0.48 — 项目类型拓展 + 立项创建/关联对外-交付

## 策略
后端集成（项目类型校验 + backfill 迁移 + initiate 创建/关联）+ 前端组件（ProjectsPage 4 类型 / DeliveryFlow 关联或新建）。E2E 真栈烟雾。

## 后端 TC

| TC | 场景 | 断言 |
|---|---|---|
| TC-PT-01 | 创建 EXTERNAL_DELIVERY | 201，projectType=EXTERNAL_DELIVERY |
| TC-PT-02 | 创建非法类型 | 400 |
| TC-PT-03 | 创建默认 | 201，projectType=CASUAL |
| TC-PT-04 | backfill FORMAL→CORE_FEATURE | seed FORMAL 行→run backfill→CORE_FEATURE，其它字段不变；NULL→CASUAL；已 CORE_TECH 不变 |
| TC-INI-01 | 立项关联 EXTERNAL_DELIVERY | WON + 该项目 → 200，opp.projectId=该项目 |
| TC-INI-02 | 立项关联 CASUAL 被拒 | WON + CASUAL 项目 → 400 |
| TC-INI-03 | 立项内联新建 | WON(含 pm) + {projectCode,projectName} → 200，新项目 type=EXTERNAL_DELIVERY 且已关联 |
| TC-INI-04 | 立项二者皆缺 | WON + 仅 decision → 400 |
| TC-INI-05 | 非 WON 立项 | OPEN → 409（不回归） |

## 前端 TC

| TC | 场景 | 断言 |
|---|---|---|
| TC-FPT-01 | ProjectsPage 类型下拉 4 项 | projects-type-select 含 4 个中文选项 |
| TC-FDH-01 | 立项关联已有 EXTERNAL_DELIVERY | 选「关联」+ 项目 → initiateOpportunity({projectId, decision:'PASS'}) |
| TC-FDH-02 | 立项新建对外-交付 | 切「新建」+ code/name → initiateOpportunity({projectCode, projectName, decision:'PASS'}) |
| TC-FDH-03 | 无项目不死路 | 无 EXTERNAL_DELIVERY → 仍可切「新建」提交 |

## E2E（docker 真栈）
- backfill：启动后既有 FORMAL 项目（若有）→ CORE_FEATURE（curl 查 projectType）。
- 创建 4 类型项目各 201；非法 400。
- 立项：建 WON 商机 → 关联已有 EXTERNAL_DELIVERY（200）/ 内联新建（200，新项目 type 正确）/ 关联 CASUAL（400）。
- 前端 bundle 含 DeliveryFlow 新建模式标记。
- 不删改既有业务数据（除 FORMAL 迁移）；throwaway 建即删。

## 回归风险
- 🟡 initiate 服务签名重构 → 同步控制器 + 既有 initiate 测试（TC-OPP 立项链项目 / 未赢单不可立项）。
- 🟢 ProjectType 加值为纯追加 + backfill 幂等；ProjectsPage 仅换共享常量。
- 🟢 DeliveryFlow 立项 UI 重构（关联/新建模式）。
