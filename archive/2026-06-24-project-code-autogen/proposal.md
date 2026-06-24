# Proposal: v0.0.49 — 项目编号自动生成（类型前缀 + 自增ID）

## Why

现创建项目要求手填项目编号（code）。用户要求：**不再输入编号**，由「项目类型前缀 + 项目自增ID」自动生成，减少输入负担、统一编号规则。

## What Changes

- 项目编号 SHALL 自动生成：`{类型前缀}-{自增id}`。类型前缀：轻量=`LT`、主业-功能建设=`CF`、主业-技术改造=`CT`、对外-交付=`ED`。
- 创建项目不再要求/接受手填 code（`ProjectCreateRequest.code` 改为可空且服务端忽略，始终自动生成）；去掉 `existsByCode` 手填查重（id 天然唯一）。
- 编号在创建时一次生成、此后**不可变**（即使后续改类型也不变，保证标识稳定）。既有项目保留其原有 code（不回填）。
- 立项内联新建对外-交付项目同样不再传 code（自动生成）；`OpportunityInitiateRequest.projectCode` 移除，新建只需名称 + 负责人。
- 前端：ProjectsPage 新建/编辑去掉编号输入框（列表仍只读展示自动编号）；DeliveryFlow 立项新建去掉编号输入。

## Capabilities

- Modified: `entity-project`（code 自动生成）、`opportunity`（立项内联新建去 code）、`frontend-scaffold`（去编号输入）。
- New: 无。

## Impact

- 代码：`ProjectType`(+前缀 map)、`ProjectService.create`(自动生成)、`ProjectCreateRequest`(code 可空/忽略)、`OpportunityInitiateRequest`(-projectCode)、`OpportunityService.resolveDeliveryProject`(去 code)；前端 `api/project.ts`/`ProjectsPage.tsx`/`api/opportunity.ts`/`DeliveryFlow.tsx` + 测试。
- 配置/数据：无新表/列/依赖；既有项目 code 不动；新项目 code = 前缀-id。

## Success Criteria

- [ ] 创建项目不传 code → 201，code = `{前缀}-{id}`（如 ED-30）；前缀按类型正确。
- [ ] 传 code 也被忽略（始终自动生成）；不再因 code 重复报 409。
- [ ] 立项内联新建对外-交付项目（仅名称+负责人）成功，项目 code = `ED-{id}`。
- [ ] 前端创建/立项表单无编号输入；列表/详情仍显示自动编号。
- [ ] 既有项目 code 不变；后端 temurin-8 全绿 + 前端全绿 + E2E 绿。
