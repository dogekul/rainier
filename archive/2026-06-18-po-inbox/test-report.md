# v0.0.42-po-inbox — 测试报告 (Phase 5 VERIFY)

> Baseline: tag `v0.0.41-admin-compliance` / commit 55bf6a8。路线图 §4 PO — 需求收件箱。

## 1. 总体概况

| 维度 | 结果 |
|------|------|
| 后端单元/集成 | **459 / 459** ✅（453 baseline + 6 new；0 fail/error/skip） |
| 前端组件/路由 | **175 / 175** ✅（171 baseline + 4 new；45 files）+ tsc clean + eslint 0 warn |
| 新增后端测试 | MeInboxControllerTest **6/6**（TC-INBOX-001..006） |
| 新增前端测试 | InboxPage **2/2** + AppRoutes /inbox **1** + navGuardConsistency 自动 +1 |
| E2E（Docker 真 MySQL） | 3/3 ✅ |
| 多路评审 (Step 0) | 3 reviewers / 7 findings / **C:0 H:0** / confirmed-real **0** |

## 2. 新增测试（MeInboxControllerTest）

待处理诉求 = 未关联且非终态（seed D2-linked + D3-closed 双负样本，断言仅 D1）/ 我的需求 owner 过滤（seed bob 需求负样本）/ projectName 富化 + 优先级排序（contains R-URG,R-LOW + R-LOW.projectName=Apollo）/ 诉求优先级排序 / 无 token→401 / token sub 无用户→两区皆空。

前端：InboxPage 两区渲染 + 双空态；AppRoutes /inbox 挂载 + literal；navGuardConsistency 自动钉 `isAdminPath('/inbox')===false`。

## 3. E2E（live stack — Docker，真 MySQL）

| # | 验证 | 结果 |
|---|------|------|
| 1 | alice `GET /api/me/inbox` | 200；unconvertedDemands 4（URGENT→HIGH→MEDIUM 排序）、myRequirements 7（alice 7/10，projectName 富化）✅ |
| 2 | 无 token | 401 ✅ |
| 3 | 存量数据（纯读） | demands 4 / requirements 10 / links 0，不变 ✅ |

## 4. 多路评审（Step 0）+ 11 类失败模式

**3 reviewers**：7 findings，**C:0 H:0**，对抗式 verify 后 **confirmed-real = 0**。核心 confirmations：软删两路 read 均经 @Where；未转化过滤正确（link 硬删 → findAll 即活链，排除 linked + 终态）；无 NPE（priorityRank(null)→末位、nullsLast、projectName null-guard）；projectName 批量富化无 N+1；me==null 两区空（降级，不 500）；Java-8 clean；测试非重言式（双负样本均 seed）；cleanDb deleteAll + @Where 隔离正确且与同类 me/* 测试一致；navGuardConsistency 自动钉 /inbox all-users。**降级一致性已确认**：proposal + spec + code 三处皆「两区皆空」。

LOW（不阻塞）：TC-INBOX-006 的 myRequirements 半边为平凡空（ghost 无 user，短路由 demands 半边证明）；InboxPage 字段级断言深度可加。

**11 类失败模式**：无幻觉；范围聚焦（me-inbox + frontend-scaffold）；契约 (k) 前后端 DTO 对齐；(d) design D1-D5 与代码吻合；无覆盖真空。

## 5. 已知取舍（记录，不阻塞）

- **待处理诉求为全局队列**：PO 分诊所有进件（非按提交人/产品 scope）；细粒度 per-product PO scope 留后续。
- **demandRepo.findAll() / linkRepo.findAll() 无分页**：应用量级（数十）可接受；增长后改 Specification NOT-IN + 分页。
- **降级**：token sub 无 User → 两区皆空（与其它 me-service 一致）。

## 6. 结论

| 信号 | 状态 |
|------|------|
| 后端 459/459 + 前端 175/175 + tsc/lint | ✅ |
| 新增 6 后端 + 4 前端测试全绿 | ✅ |
| E2E inbox（排序/owner/富化）+ 401 + 数据零改 | ✅ |
| Docker 真 JDK-8 构建 | ✅ |
| 多路评审 C:0 H:0 confirmed-real:0 | ✅ |
| PO 角色落地（最弱角色翻通） | ✅ |

**部署建议**：可交付（用户已预授权 P6 + push）。后续：per-product PO scope、未转化诉求分页、一键转需求动作。
