# Project Implementation Form (D1) — proposal

## 现状
- `handoff-advance` (archive/2026-06-24) 已把 Opportunity 的最终验收节点切换成自动建 `Project`（合同已签 + 实施中）。
- 项目立项后缺少一个「施工内容」结构化记录入口：施工范围、估工时、关键里程碑、风险备注。
- 现状 Project 只承载身份字段（owner / pmo / 团队 / 日期 / 状态），扩展信息没地方落。

## 目标（v0.0.89）
- 引入 1:1 的 `ProjectImplementation`，按 `projectId` 唯一，作为「施工内容」的结构化容器；
- 提供 `GET / PUT /api/projects/{id}/implementation` 上行 upsert，幂等；
- 前端「项目详情」加一个「施工内容」Tab，简单 markdown + 表单。

## OutOfScope
- 不自动把 scope/milestones 拆解为 Sprint/Story；
- 不做模板库（保留下个 sub-change）。

## 数据模型
`rainier_project_implementation`:
- `id`, audit (`BaseEntity`)
- `project_id` BIGINT NOT NULL UNIQUE
- `scope_markdown` LONGTEXT NOT NULL
- `estimated_man_days` INTEGER NULL
- `risk_notes` VARCHAR(2000) NULL
- `key_milestones_json` LONGTEXT NULL — JSON 数组字符串（前端解析，后端只做透传）

## 端点
- `GET /api/projects/{projectId}/implementation` — 取一条；不存在返回 `404`。
- `PUT /api/projects/{projectId}/implementation` — upsert；body 含 `scopeMarkdown` 必填 + 其余可选。
- 项目不存在 → `400`。

## 测试
- `ProjectImplementationServiceTest.createOrUpdate_idempotent`
- `ProjectImplementationControllerTest`: GET 空 → 404；PUT → 200/201；再 PUT 同一 projectId → 同一 id（id 不变 = upsert）。
