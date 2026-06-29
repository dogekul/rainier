# spec — crm-full-link-visualization (v0.0.94)

## Endpoint
`GET /api/opportunities/{id}/full-link` — all-users (token-optional)

### Response `FullLinkResponse`
```
{
  opportunity:   { id, customerName, title, stage, status, projectId, customerId, ... },
  customer:      { id, name, industry } | null,
  project:       { id, code, name, status, projectType, ownerUserId } | null,
  operation:     { id, customerName, title, stage, status, opsOwnerUserId } | null,
  presaleStages: [ { code, label, current, activityCount, doneCount, artifactCount } ],
  deliveryStages:[ { code, label, current, activityCount, doneCount, artifactCount } ]
}
```

- `presaleStages` 覆盖 LEAD..CONTRACT (5)；`deliveryStages` 覆盖 INITIATION..ACCEPTANCE (5)
- `current=true` 标记 opportunity 当前 stage 对应的那一段
- `activityCount/doneCount` 来自 StageActivity (按 stageCode)
- `artifactCount` 来自 OpportunityArtifact（暂归口到 opportunity 维度，全段总计映射到 product-stage code 维度）
  - 简化版：用 artifact `type` 不易精确归到 stage，本期 artifactCount = StageActivity 关联 artifact 字段数，
    若无则置 0；不阻塞 timeline UX
- 任一段无对应实体时返回 null（如 project 还没立项）

## Scenarios
1. **Scenario A — 完整链**：seed opportunity at ACCEPTANCE，linked project + operation + customer + activity records → 返回所有字段非空，presale 全部 `done`，delivery `current=ACCEPTANCE`
2. **Scenario B — 早期链**：seed at LEAD，无 project / operation / customer → 返回 project=null / operation=null / customer=null，presale 第一段 `current=true`
3. **Scenario C — 不存在 opportunity** → 404

## 前端
- `OpportunityDetailPage` 新增一个「全链」section（条件渲染：`opp` 已加载后）
- 渲染三大段卡片：商机 / 项目（若有）/ 运营（若有）以 CSS Timeline 串联
- 每段含：标题 + key/value 摘要 + 跳转锚（商机当前页内、项目跳 `/projects/{id}`、运营跳 `/operations/{id}` 若已存在路由；否则纯展示）
