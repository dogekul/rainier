# Capability: frontend-scaffold — v0.0.56 opp-requirement-gen delta (MODIFIED)

> 合并入 canonical `specs/frontend-scaffold/spec.md`（Phase 6）。商机详情页助手式生成诉求/需求。见 [[opportunity]]。

## MODIFIED Requirements (from change 2026-06-25-opp-requirement-gen / v0.0.56)

### Requirement: 商机详情页据调研+产品助手式生成诉求/需求

商机详情页 SHALL 提供「生成产品诉求/需求」动作：打开草稿表单，**客户端预填**（无 LLM）标题=`客户·标题`、描述=聚合 现场调研报告正文 + 现场调研附件链接 + 产品名 + 来源商机标注；SHALL 可切换「提交为 诉求(Demand) / 需求(Requirement)」并可编辑；提交 SHALL 以 `createDemand`/`createRequirement` 创建并带 `opportunityId`。详情页 SHALL 列出本商机已生成的诉求/需求（按 opportunityId 拉取）。

#### Scenario: 生成草稿预填调研+产品

- **GIVEN** 商机有 现场调研报告/附件 与 产品名
- **WHEN** 点「生成产品诉求/需求」
- **THEN** SHALL 打开 `opp-gen-form`，描述预填含调研正文与产品名

#### Scenario: 提交为诉求

- **GIVEN** 草稿已打开、目标=诉求
- **WHEN** 编辑后点提交
- **THEN** SHALL 调 `createDemand`，body 含 `opportunityId` 与编辑后的标题/描述

#### Scenario: 切换为需求提交

- **GIVEN** 草稿已打开
- **WHEN** 切到「需求」并提交
- **THEN** SHALL 调 `createRequirement`，body 含 `opportunityId`

#### Scenario: 已生成区列出派生项

- **GIVEN** 商机已派生 1 诉求 + 1 需求
- **WHEN** 详情页渲染
- **THEN** SHALL 出现 `opp-gen-list`，含该诉求与需求
