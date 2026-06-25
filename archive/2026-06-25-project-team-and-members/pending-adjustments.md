# Pending Adjustments — v0.0.64

## PA-1: AuditorAwareImpl 修正读属性 key

**原始设计**：未涉及 — 假定 BaseEntity.createBy/updateBy 自动获取真实登录名

**实际偏离**：发现 `AuditorAwareImpl` 读 request 属性 `"username"`，而 `SecurityFilter` 设置的是 `"rainier.username"`。两者不匹配 → 生产环境下所有 BaseEntity.createBy/updateBy 实际填 `"system"`，不是真实登录名。这是 v0.0.15 引入审计时遗留 bug。

**调整**：`AuditorAwareImpl.getCurrentAuditor()` 优先读 `"rainier.username"`（canonical 来源），fallback 读 `"username"` 兼容老代码（无人填它）。

**为什么需要**：
- v0.0.64 引入的 `ProjectMember.joinedBy` 字段也通过 AuditorAware 取值；不修就只能填 "system"
- ProjectMemberControllerTest TC-PMEM-001 测 `$.joinedBy="lina"` 验证身份；bug 不修则测试失败
- 顺手修复改善整个系统 createBy/updateBy 准确性（之前所有 audit_log.create_by 写的是 "system"）

**影响范围**：
- 1 个文件：`backend/src/main/java/com/rainier/common/persistence/AuditorAwareImpl.java`
- 行为变化：实际登录用户做的任何 BaseEntity create/update 现在会正确记 createBy/updateBy = login_name（之前都是 "system"）；幂等向下兼容（fallback 仍读 "username"）
- 测试：既有 audit_log 测试 spot-check 可能从期待 "system" 变成期待真实 login_name；运行验证

**记录时间**：2026-06-25 / Phase 4 BUILD batch 3 测试发现
