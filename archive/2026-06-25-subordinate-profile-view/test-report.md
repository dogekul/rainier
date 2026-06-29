# Test Report: C3 subordinate-profile-view (v0.0.83)

## Backend
- Command: `cd backend && mvn test`
- Result: **734 tests, 0 failures, 0 errors, 0 skipped — BUILD SUCCESS**
- 时长: ~13.6s

## 新增测试 (9 个 — UserProfileControllerTest)

| TC | 场景 | 期望 | 实际 |
|---|---|---|---|
| TC-SUBPROF-001 | caller==target → 自助读 | 200 + 完整 profile | PASS |
| TC-SUBPROF-002 | caller 是 target team HEAD | 200，body.userId==target | PASS |
| TC-SUBPROF-003 | caller 是 target primary org 父 org HEAD | 200 | PASS |
| TC-SUBPROF-004 | 同 team MEMBER 互看（无上下级） | 403 | PASS |
| TC-SUBPROF-005 | 完全不相关用户互看 | 403 | PASS |
| TC-SUBPROF-006 | target 不存在 | 404 | PASS |
| TC-SUBPROF-007 | 无 token | 401 | PASS |
| TC-SUBPROF-008 | token sub 在 user 表无匹配 (ghost) | 403 | PASS |
| TC-SUBPROF-009 | target 无在岗 membership，非 self 访问 | 403 | PASS |

## 既有回归
- `MeProfileControllerTest` 7 个用例全部继续通过（refactor 后 `profileOf(String)` 行为不变）
- 全量 backend 734 个测试无失败

## 未跑
- 前端：本切片无前端改动（仅后端 API + service）。

## 覆盖说明
- 「直接上级」判定只看 target 的 primary org + 其父 org（1 层），与 OutOfScope 一致
- HR/Admin 直通、多级上级递归不在本切片范围
