# Spec: D7 — 运营 PO scope + 问题分页 + 转工单 (v0.0.95)

## Scenario A: /api/me/inbox?productId 过滤
- Given alice owns req R1 (opp O1, product P1), R2 (opp O2, product P2), R3 (no opp)
- When GET /api/me/inbox?productId=P1 with alice token
- Then myRequirements contains only R1; unconvertedDemands is empty

## Scenario B: 不传 productId 保持原行为
- When GET /api/me/inbox (no productId)
- Then unconvertedDemands + myRequirements 全部按原规则返回

## Scenario C: 运营问题分页
- Given Operation Op1 has 25 issues
- When GET /api/operations/{Op1}/issues/page?page=0&size=10
- Then content size=10, total=25

## Scenario D: 分页 + status 过滤
- Given mixture of OPEN / CLOSED issues
- When GET .../issues/page?status=OPEN
- Then only OPEN returned, total counts only OPEN

## Scenario E: convert-to-task 创建 Task 并标记 CONVERTED
- Given issue I1 (title="灯泡坏", desc="X 楼"), project P1 exists
- When POST /api/operation-issues/{I1}/convert-to-task {projectId: P1}
- Then 201 with task body; task.title=="灯泡坏", task.description=="X 楼", task.projectId=P1
- And GET /api/operation-issues/{I1}.status == "CONVERTED"

## Scenario F: convert-to-task — project 不存在 → 400
- When POST .../convert-to-task {projectId: 999999}
- Then 400 BadRequest

## Scenario G: convert-to-task — issue 不存在 → 404

## Test IDs
- TC-INBOX-PROD-001/002 (Scenarios A/B)
- TC-OPI-PAGE-001/002 (Scenarios C/D)
- TC-OPI-CONV-001/002/003 (Scenarios E/F/G)
