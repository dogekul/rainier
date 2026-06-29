# Proposal: C3 下属 profile 视图（本人 + 直接上级）

## Problem
v0.0.40 引入 `GET /api/me/profile` 只覆盖本人自助场景（archive/2026-06-18-me-profile）。
团队负责人想看直接下属的档案（岗位/组织/直接上级/贡献计数）目前没有入口，
唯一选项是 `/api/users/{id}` 的薄 UserDetail，缺 manager / 贡献计数。

## Decision
新增 `GET /api/users/{id}/profile`，复用 `MeProfileService` 的聚合逻辑（抽公共
`profileOfUserId(Long)` 出来，`profileOf(String)` 委托过去）。新增 controller
`UserProfileController`（与 `UserController` 同 base path `/api/users` 但子路径
`{id}/profile`，避免与 `GET /api/users/{id}` 冲突；Spring 精确路径匹配优先）。

鉴权矩阵：

| 调用者 vs 目标 | 结果 |
|---|---|
| 未带 token | 401 |
| target 不存在 | 404 |
| caller.id == target.id | 200（同 /api/me/profile 内容）|
| caller 是 target「直接上级」 | 200 |
| 其他 | 403 |

「直接上级」定义（取自 me-profile 的 resolveManager 行为）：
- 取 target user 的 primary 在岗 organization（无 primary 则取第一条在岗 membership）
- 在该 organization 的在岗 HEAD（非 target 本人）中如果有 caller → 200
- 否则上溯 1 层（target primary org 的 parentId 对应 organization）的在岗 HEAD 中如果有 caller → 200
- 否则 403

⚠️ 与 OutOfScope「仅直接上级」保持一致：最多上溯 1 层（直属 org HEAD + 父 org HEAD）。

## Non-Goals
- 多级上级递归（v0.0.40 me-profile 的 manager 上溯最多 8 层是 self-only 场景；这里
  下属访问只覆盖直属 + 父 1 层，超出范围的需求待 HR/PMO 角色专用端点解决）
- HR 总监 / Admin 无差别下属访问（Admin 已有 /api/admin/* 通道；HR 待 D 批）
- 写权限（仅 GET）
- 修改既有 `/api/me/profile` 响应结构

## Compatibility
- 既有 `MeProfileController` 行为不变（依旧调 `profileOf(String)`）
- 既有 `MeProfileControllerTest` 必须全绿
- `GET /api/users/{id}` 老端点不动；`{id}/profile` 子路径不冲突
