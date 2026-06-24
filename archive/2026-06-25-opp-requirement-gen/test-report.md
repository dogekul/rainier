# Test Report — v0.0.56 商机→诉求/需求生成 + 转化卡点 + 详情页布局优化

> 迭代 v2（用户反馈后）：① 详情页布局重排（全宽单列 + 多列字段 + 强 hero + hero/字段分隔）；② 新增「产品诉求→交付实施」推进卡点 = 该商机须已有 ≥1 条需求（Option B），否则 advance 400「请先将诉求转化为需求…」。**后端 temurin-8 539→541**（+TC-OREQ-01/02；TC-OSEA-02 / TC-OAR-006 因 REQUIREMENT 现有卡点改用 DELIVERY→ACCEPTANCE 作非门禁样例）。**前端 260/260**，tsc/lint clean。无新表/列（沿用 opportunity_id）。

---

## （初版）Test Report — v0.0.56 商机→产品诉求/需求生成

## 1. 总体概况

| 层 | 总数 | 通过 | 失败 | 通过率 |
|----|------|------|------|--------|
| 后端 (temurin-8 全量) | 539 | 539 | 0 | 100% |
| 前端 (Vitest) | 260 | 260 | 0 | 100% |
| tsc / eslint | clean | — | — | — |
| E2E (live MySQL+HTTP) | 链路 | 通过 | 0 | — |

后端 532→539（+7：TC-OGEN-D1/D2/D3/D4 + R1/R2/R3）。前端 256→260（+4：TC-OGEN-F1..4）。

## 2. 按模块

- **DemandControllerCreateTest**: +TC-OGEN-D1（带 opportunityId→201+返回）/D2（不存在→400）/D3（不带→null）。
- **DemandControllerQueryTest**: +TC-OGEN-D4（?opportunityId= 过滤）。
- **RequirementControllerCreateTest**: +TC-OGEN-R1/R2。
- **RequirementControllerQueryTest**: +TC-OGEN-R3。
- **OpportunityDetailPage.test.tsx**: +TC-OGEN-F1（生成草稿预填调研+产品）/F2（提交诉求带 opportunityId+submitter）/F3（切需求提交带 opportunityId+owner）/F4（已生成列表按 opportunityId 拉取）。

## 3. E2E（live）

- `SHOW COLUMNS`：rainier_demand / rainier_requirement 均含 `opportunity_id bigint NULL`（ddl-auto 加列成功）。
- `POST /api/demands` {opportunityId:46} → 201，返回 opportunityId=46；`POST /api/requirements` 同 → 201。
- `GET /api/demands?opportunityId=46` → total=1（过滤正确）。
- `POST /api/demands` {opportunityId:99999999} → 400（不存在校验）。
- 清理 throwaway opp 46 + 其派生 demand/requirement；真实数据未动。

## 4. 失败项

无。（E2E 调试期一次 400「submitter user not found」系测试用错用户 id，非缺陷——恰证明 submitterUserId 校验生效。）

## 5. 功能/测试覆盖对照

| 功能 | 实现 | 测试 |
|------|------|------|
| opportunityId 持久化/校验/返回 | Demand/Requirement entity+DTO+service | TC-OGEN-D1/R1, E2E |
| 不存在 opportunityId → 400 | existsById 校验 | TC-OGEN-D2/R2, E2E |
| list 按 opportunityId 过滤 | Spec 谓词 | TC-OGEN-D4/R3, E2E |
| 助手式草稿预填 | composeDraft(调研+产品) | TC-OGEN-F1 |
| 提交诉求/需求 | createDemand/createRequirement | TC-OGEN-F2/F3 |
| 已生成列表 | listDemands/Requirements by opportunityId | TC-OGEN-F4 |

## 6. 设计调整

见 design-adjustments.md：评审 C0 H0 M0 L3；补 composeDraft 描述截断（避免超 @Size 400）。

## 7. 多路评审（Step 0，单代理对抗审查）

C:0 H:0 M:0 L:3。核验：existsById 含软删语义（可接受）；Requirement enrich 路径保留 opportunityId（TC-OGEN-R1 端到端验证）；update 不改 opportunityId（不可变，符合既有 projectId 区分）；list 谓词正确；无未更新调用方；前端 null 安全/owner 兜底/code 唯一/列表刷新/跨商机 state 复位均正确；测试有判别力无假绿。L1（任意 opportunityId，仅追溯元数据，匹配既有无鉴权写）/L2（priority 总发，无害）记录不改；L3（超长描述）→ 已加客户端截断至 1900。

## 8. 结论

后端 539 + 前端 260 全绿、tsc/lint clean、live 列迁移+创建+过滤+校验验证通过、存量数据未改。评审零 C/H/M。建议进入 Phase 6 交付。
