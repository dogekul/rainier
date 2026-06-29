# Proposal: B3 密码管理 — 改密 / 重置 / 找回

## Problem
v0.0.38 引入 BCrypt + default-password backfill 后，缺少：
- 自助改密（用户带 currentPassword）
- 管理员重置（admin override，绕过 currentPassword）
- 找回密码（无 token 用户走「loginName + email 匹配」+ 一次性 reset token）

## Decision
新增 `password` 包，4 个端点 + 1 个 entity:

| 端点 | 鉴权 | body | 行为 |
|---|---|---|---|
| POST /api/me/password | 已登录 | {currentPassword,newPassword} | 校验旧密码，更新 hash |
| POST /api/admin/users/{id}/reset-password | admin (Tier A) | {newPassword} | 直接覆盖目标 hash |
| POST /api/auth/forgot-password | 无 token (whitelist) | {loginName,email} | 校验 user+email 匹配，签发 1h token；不发邮件，log 打印 |
| POST /api/auth/reset-password | 无 token (whitelist) | {token,newPassword} | 校验 token 未过期/未用，覆盖 hash + 标记已用 |

新表 `rainier_password_reset_token`: id, user_id, token (UUID), created_at, expires_at, used_at?

**newPassword 策略**：非空 + 长度 ≥ 8（不做大小写/字符类校验）。

## Non-Goals
- 邮件发送（stub：log 打印 token）
- 历史密码防重用
- 强密码策略
- 限流 / 防枚举

## Compatibility
- `app.security.real-auth.enabled=false` 时 /api/me/password 仍可调（依然校验 BCrypt hash），但 forgot/reset 适用于所有 profile
- AdminPaths 扩展：在 /api/admin 这条新前缀加入 Tier A（reset-password 端点必须 admin）
- forgot/reset 在 `SecurityFilter` 白名单内 — 注意：避免泄露 user 是否存在 → 即使 user 不存在或 email 不匹配也返回 200（静默）
