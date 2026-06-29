# Test Report — C8 项目成员多角色 + bulk add (v0.0.88)

## 命令
`cd backend && mvn -o test`

## 结果
- Tests run: **794, Failures: 0, Errors: 0, Skipped: 0**
- BUILD SUCCESS

## 本 change 新增/触及
| Test | 用例数 | 状态 |
|---|---|---|
| `ProjectMemberBulkAddTest` | 6 | 全绿 |
| `ProjectMemberRoleAssignmentControllerTest` | 6 | 全绿 |
| `ProjectMemberRoleBackfillTest` | 1 | 全绿 |
| `ProjectMemberControllerTest` (既有 12 个不动) | 12 | 全绿（兼容 OK）|
| `LegacyProductCategoryCleanupTest` | 38→39 表 | 调整后通过 |

## 覆盖
- bulk add 笛卡尔积 / merge / owner-skip / 非法 role / 空 list 校验
- role assignment add / list / remove / dup-409 / invalid-400 / unauthorized-403
- backfill：legacy ProjectMember.role 启动同步 + idempotent
- DTO `roles[]` 富化在新旧路径都被验证（bulk 返回 jsonPath `$[0].roles.length()`）

## Caveats
- 既有 `ProjectMemberControllerTest` 未清 `roleRepo`，但因为 PM `repo.deleteAll()` 已置空业务上下文，残余 assignment 行不影响 12 个原用例（已实跑确认）。
- bulk add 对 owner 是「静默跳过」非「跳过+返回」，spec 显式如此。
- single-add (`create()`) 与 single-update (`update()`) 现在会同步写一条 `ProjectMemberRoleAssignment`，向后兼容（旧前端读取仍稳定）。
