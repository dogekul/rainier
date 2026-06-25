# Design Adjustments — v0.0.64

## DA-1: AuditorAwareImpl 修正 request 属性 key 读取顺序 (from PA-1)

**原始设计**：未涉及 — 假定 BaseEntity.createBy/updateBy + project_member.joined_by 自动获取真实登录名。

**实际偏离**：实现 ProjectMemberControllerTest TC-PMEM-001 验证 `joinedBy="lina"` 时失败，发现：
- `AuditorAwareImpl` 读 request 属性 `"username"`
- `SecurityFilter` 设置的是 `"rainier.username"` (canonical key, AuthController.ATTR_USERNAME)
- 两者不匹配 → 生产环境下 AuditorAware 始终返回 fallback `"system"`，BaseEntity 审计字段从未填真实用户名
- v0.0.15 引入审计后遗留的 dormant bug

**实际调整**：`AuditorAwareImpl.getCurrentAuditor()` 改为：
- 优先读 `"rainier.username"` (canonical)
- 缺失则读 `"username"` (fallback 兼容旧调用方)
- 都缺则返回 `"system"`

**为什么需要**：
- v0.0.64 引入 `ProjectMember.joinedBy` 字段也通过 AuditorAware 取值 → 若不修则只能填 "system"，价值丧失
- TC-PMEM-001 spec 显式断言 `joinedBy="lina"` → 测试无法通过
- 顺手改善整个 BaseEntity.createBy/updateBy 准确性（所有 audit_log 历史记录的 actor 都是 "system"）

**影响范围**：
- 1 个文件：`backend/src/main/java/com/rainier/common/persistence/AuditorAwareImpl.java`
- 行为变化：实际登录用户做的任何 BaseEntity create/update 此后正确记录 createBy/updateBy = login_name
- 向下兼容：fallback 读 `"username"`，理论上不破坏任何调用（实际无人填该 key）
- 测试影响：既有 audit_log spot-check 测试若有 hardcoded "system" 断言可能需调整 → 验证 Phase 5 Step 1 backend 566/566 测试全通过，确认无回归

**记录时间**：2026-06-25 / Phase 4 BUILD M06 测试发现 / Phase 5 Step 0 review 确认无遗留风险

## 越界范围说明（Step 3 失败模式 b 触发）

本次变更包除 proposal.md "Impact" 列出的文件外，**还触及 `AuditorAwareImpl.java`**（不在原始 Impact 中）。属于「无意发现且修复 dormant bug」类越界：
- 范围小（1 个 5 行的逻辑改）
- 风险低（fallback 保留）
- 收益高（修复 v0.0.15 以来的审计日志失真）
- 已 backend 566 测试覆盖确认无回归

判定：**接受越界**（属 "good developer improves code they're working in" 范畴，不需拆分独立 change）。

## DA-2: ProjectDetailPage 编辑改为 Tab 内联，不再用侧抽屉

**原始设计**：`ProjectDetailPage` 「编辑」按钮 onClick → 打开 `ProjectEditDrawer` 侧抽屉编辑（v0.0.62 沿用，本版扩展了 team/PMO 字段）。

**用户反馈（Gate 3 期间）**：「项目的编辑页 不要使用侧抽屉」。

**实际调整**：
- 删除 `ProjectDetailPage` 中的 `<ProjectEditDrawer>` 挂载和 `drawerOpen` state
- 增加 `editing: boolean` state + 完整表单 state（`eName / eDescription / eStatus / eProjectType / eOwnerUserId / eOrganizationId / ePmoUserId / pmoCandidates / pmoLoading / eStartDate / eEndDate / eEnabled / eError / eSaving / lastPmoReqId`）
- 点击 Hero「编辑」按钮 → `setEditing(true)` + 预填表单 + 拉 users / orgTree
- editing=true 时「基本信息」Tab 渲染**内联表单**（替代 readonly grid），与 OperationDetailPage v0.0.59 编辑模式同样的范式
- Hero 显示「保存」/「取消」按钮（替代「编辑」）
- owner change → 自动取主组织；team change → 拉 effective-PMOs 重列；与之前 Drawer 中相同的联动逻辑迁移到本组件
- `ProjectsPage` 上「新建项目」仍用 `ProjectEditDrawer`（创建场景短，侧抽屉合适）

**为什么**：
- 与用户在 v0.0.61 提的反馈一致：「打开侧抽屉或当页展开的逻辑太不友好」
- 编辑是大表单（9 个字段 + 异步联动），全屏 Tab 比侧抽屉更友好
- 与 v0.0.59 OperationDetailPage 编辑模式一致（编辑→内联表单→保存/取消）

**影响范围**：
- 1 个文件：`frontend/src/pages/Project/ProjectDetailPage.tsx`（~150 行 inline edit form 注入）
- 1 个测试更新：`ProjectsPage.test.tsx` TC-FES-P04 改为针对新 testids（`project-detail-edit-owner` / `project-detail-edit-save`）
- 测试：tsc clean / 265/265 vitest / eslint clean
- `ProjectEditDrawer.tsx` 仍保留（ProjectsPage 新建用），无改

**记录时间**：2026-06-25 / Phase 5 Gate 3 用户反馈即时调整
