# D2 — 商机各节点活动清单 + 输出物关联面板 (v0.0.90)

## 背景
- PresaleFlow 节点（LEAD/OPPORTUNITY/POC/SURVEY/CONTRACT/...）记录商机当前阶段。
- OpportunityArtifact 已是每节点的「产出物」。
- 缺失：每个 stage 内「人在做什么」(活动清单) + 产出物快查的整合视图。

## 范围（D2）
- 新建 StageActivity (rainier_stage_activity) 实体：opportunityId / stageCode / activityTitle / description / assigneeUserId / dueDate / status / completedAt。
- 服务：list / add / markDone / skip。
- 端点：
  - GET  /api/opportunities/{id}/stages/{code}/activities
  - POST /api/opportunities/{id}/stages/{code}/activities
  - POST /api/stage-activities/{id}/done
  - POST /api/stage-activities/{id}/skip
  - GET  /api/opportunities/{id}/stages/{code}/dashboard → {activities, artifacts}
- 前端：商机详情页加「{当前 stage} 活动清单 + 关联产出物」section（轻量整合视图）。

## OutOfScope
- 模板（按 stage 自动填充活动）。
- 活动→Task 转化。
