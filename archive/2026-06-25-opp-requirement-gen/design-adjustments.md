# Design Adjustments — v0.0.56 opp-requirement-gen

Phase 5 评审（Step 0 单代理对抗审查）：**C:0 H:0 M:0 L:3**。无阻塞缺陷。

## 已改（评审 L3）
- **composeDraft 描述截断**：预填描述拼接 现场调研报告 + 附件 + 产品 可能超后端 `@Size`（Demand 2000 / Requirement 4000）。加客户端截断至 1900 字 + 「…（已截断，可补全）」提示，避免大调研报告导致提交 400。`OpportunityDetailPage.composeDraft`。

## 记录未改（评审确认可接受）
- **L1 任意 opportunityId**：客户端可对任一存在商机建诉求/需求 —— 仅追溯元数据、无鉴权门、无级联，匹配这些端点既有「无 per-entity 鉴权」写入姿态。若未来端点加鉴权再收口。
- **L2 priority 总是下发**：即使默认 MEDIUM 也发送，后端默认相同，无害。
- existsById 含软删语义（软删商机视为不存在 → 400）：可接受、与设计一致。
- update 不携带/不改 opportunityId（创建后不可变）：与既有 projectId（可变）有意区分，安全。

## 设计意图（无 LLM）
- 「生成」= 客户端据 现场调研产出物(SURVEY_REPORT 正文 + SURVEY_ATTACHMENT 链接) + 产品名 组合草稿；用户审改后提交为 诉求(Demand) 或 需求(Requirement)，带 opportunityId 关联回商机。后端无 LLM/新依赖。详情页「已生成」区按 opportunityId 回看派生项。
