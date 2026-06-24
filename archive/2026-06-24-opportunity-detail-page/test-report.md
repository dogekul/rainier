# Test Report — v0.0.55 统一商机详情页 + CRM 商机页面视觉优化

> 补充（视觉优化阶段）：在统一详情页之上，对 4 个 CRM 商机页面做卡片化视觉优化（商机看板/商机详情页/售前流转/实施流转），全部复用 `global.css` 设计令牌 + board 组件，**所有 data-testid 保持不变**。最终前端 **256/256** 全绿、tsc/lint clean。改动：新增 `OpportunityDetailPage.css`、`oppFlow.css`；重构 DetailPage 为卡片化(头像+chip+字段网格+产出物卡)；PresaleFlow/DeliveryFlow 表格→行卡片(hover-lift)；OpportunityBoard 漏斗条/阶段泳道/列表→卡片化 + 阶段色左强调。无逻辑/契约改动，纯展示层。

---

## （原）Test Report — v0.0.55 统一商机详情页

## 1. 总体概况

| 层 | 总数 | 通过 | 失败 | 通过率 |
|----|------|------|------|--------|
| 前端 (Vitest) | 256 | 256 | 0 | 100% |
| tsc | — | clean | — | — |
| eslint | — | clean | — | — |
| 后端 | 无改动 | — | — | — |
| E2E (live) | GET /{id} 200 + SPA 路由 200 | 通过 | — | — |

前端 255→256（v0.0.54→v0.0.55 净变化：−5 TC-DDET +1 TC-DEL-NAV；−4 PresaleFlow detail +1 TC-PRE-NAV；+8 OpportunityDetailPage）。测试文件 54→55。纯前端，后端零改动。

## 2. 按模块

- **OpportunityDetailPage.test.tsx (NEW)**: 8/8 — 加载(TC-ODP-01)/产出物列表(02)/编辑保存+customerId匹配(03)/添加报告(04)/加载错误态(05)/返回(06)/添加链接类(07)/非法id(08)。
- **PresaleFlow.test.tsx**: 移除 4 个抽屉测试(TC-PAR-04/PDE-01/02/03 — 功能已迁至详情页)，新增 TC-PRE-NAV（行详情→跳转）。推进/关口/补充/create 全部保留绿。
- **DeliveryFlow.test.tsx**: 移除 5 个抽屉测试(TC-DDET-01..05)，新增 TC-DEL-NAV。立项移交/现场调研补充/推进保留绿。

## 3. E2E（live）

- `GET /api/opportunities/{id}`（详情页数据源，已存在端点）→ 200。
- `http://localhost/crm/opportunities/{id}`（SPA 路由）→ 200。
- 前端新镜像已部署(:80)。无后端改动、无数据写入。

## 4. 失败项

无。

## 5. 功能/测试覆盖对照

| 功能 | 实现 | 测试 |
|------|------|------|
| 详情页按 id 加载 | OpportunityDetailPage getOpportunity+listArts | TC-ODP-01, E2E |
| 产出物列表(导出/链接) | 列表渲染 | TC-ODP-02/07 |
| 编辑保存 | saveDetail(updateOpportunity) | TC-ODP-03 |
| 添加产出物(报告/链接) | submitAddArtifact | TC-ODP-04/07 |
| 错误/非法id/返回 | loading/error/navigate(-1) | TC-ODP-05/06/08 |
| 行详情跳转 | navigate('/crm/opportunities/:id') | TC-PRE-NAV, TC-DEL-NAV |
| 推进/门禁不回归 | 保留行上 | PresaleFlow/DeliveryFlow 既有用例 |

## 6. 设计调整

见 design-adjustments.md：评审无 C/H/M；补 TC-ODP-07/08 覆盖链接类添加与非法 id 守卫（评审 Low）。

## 7. 多路评审（Step 0，单代理对抗审查）

C:0 H:0 M:0 L:3（均非缺陷正向确认/dev-only）。重点核验：
- 抽屉删除无回归 —— advanceFromDetail 仅转调 requestAdvance（行上同函数），推进各阶段行上覆盖齐全；无悬挂引用/死代码/未用 import。
- 路由 `/crm/opportunities/:id` 与 `/crm/opportunities`(看板) 不冲突（v6 静态段优先）。
- 深链自足（按 id fetch，不依赖列表 state）。
- 评审建议补的两个未测分支（链接类添加 / 非法 id）→ 已补 TC-ODP-07/08。
- 范围外观察：看板(OpportunityBoardPage) 未接入详情页跳转（Gate 1 已定「看板暂不接入」）。

## 8. 结论

前端 256 全绿、tsc/lint clean、后端零改动、live 路由+数据源验证通过；评审零 C/H/M，Low 已补测。取代两页详情抽屉为统一详情页。建议进入 Phase 6 交付。
