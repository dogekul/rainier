# Design: v0.0.46 — 投标→合同→立项 产出物门禁

## Context

阶段机（`OpportunityStage`）：LEAD→OPPORTUNITY→POC→BIDDING→CONTRACT→INITIATION→…。GATE_STAGES =
{OPPORTUNITY, BIDDING, CONTRACT, INITIATION}；PRESALES_GATES = {OPPORTUNITY, BIDDING, CONTRACT}（REJECT→LOST）。
`advance()` 在「关口 decision 校验」后调 `persistRequiredArtifact(o, stage, decision, artifact)`，再做 REJECT/PASS 分支
（CONTRACT PASS → INITIATION + WON）。产出物规则在 `TransitionArtifactRules.RULES`（keyed by SOURCE stage）。
产出物模型支持 link-kind（`link` URL）/ report-kind（`content` 富文本），`POST /{id}/artifacts` 独立提交、title 可空兜底。

用户 Gate-1 决策：附件**先 URL 占位**（无文件上传/邮件基建）；邮件留痕 = 上传归档邮件作链接；合同评审 = 仅产出物。

## Decisions

### D1 — 6 个新 ArtifactType（5 链接 + 1 报告）

| 常量 | 标签 | kind |
|---|---|---|
| `BID_DOCUMENT` | 投标文件 | link |
| `BID_WINNING_NOTICE` | 中标公示 | link |
| `CONTRACT_DRAFT` | 合同 | link |
| `CONTRACT_REVIEW_MINUTES` | 评审会议纪要 | **report** |
| `REVIEW_EMAIL_ARCHIVE` | 邮件归档 | link |
| `SIGNED_CONTRACT` | 已盖章合同 | link |

评审会议纪要取 report-kind（与 `DECISION_MINUTES` 同类，可在系统内富文本编辑 + Word 导出）；其余 5 类取 link-kind
（附件先贴 URL）。加入 `ALL`/`LABELS`，链接 5 类加入 `LINK_TYPES`。

**为什么不引入 FILE kind**：用户选 URL 占位。FILE kind 需新表/列 + multipart + 下载端点 + Docker 卷（全无基建），推迟到对象存储迁移。
URL 占位让本版零新基建、完全复用 v0.0.45 链接类管线。

### D2 — 2 条新门禁规则（`TransitionArtifactRules.RULES`）

- `BIDDING → [BID_DOCUMENT]`（单类型，但 link-kind → 走 Path B，见 D4）。
- `CONTRACT → [BID_WINNING_NOTICE, CONTRACT_DRAFT, CONTRACT_REVIEW_MINUTES, REVIEW_EMAIL_ARCHIVE, SIGNED_CONTRACT]`（5 类，Path B）。

advance 逻辑不动——规则纯数据新增（该文件本就为此设计）。

### D3 — 门禁仅 PASS（前进）强制；关口 REJECT 跳过；商机决策例外

`persistRequiredArtifact` 当前对 PASS/REJECT 都跑——这对「丢单」错误（投标失败时不应要求《已盖章合同》）。改为：

```
若 stage∈GATE_STAGES 且 decision==REJECT 且 !requiredOnReject(stage) → 直接 return（不要求产出物）。
```

`TransitionArtifactRules.requiredOnReject(stage)` 仅对 `OPPORTUNITY` 返回 true（《决策评审纪要》记录通过/否决，两种结果都要留痕）。
投标/合同 REJECT 因此不要求产出物，丢单照常。非关口阶段（LEAD/POC/实施段）decision 为 null，不受此分支影响。

**备选**：给每条规则加 `onReject` 标志——更通用但当前只有 OPPORTUNITY 需要，单一 set 更简。

### D4 — Path A 单产出物内联创建加 `!isLink` 守卫

现 Path A 条件：`required.size()==1 && artifact!=null && title/content 非空` → 内联建 report-kind 产出物。BIDDING 规则
size==1 但 BID_DOCUMENT 是 link-kind——若误走 Path A 会把链接类当报告类（写 content）建错。加 `&& !ArtifactType.isLink(required.get(0))`：
链接类单一规则永远走 Path B（须经 `POST /{id}/artifacts` 预提交，前端补充表单负责）。LEAD/OPPORTUNITY（report 单类）行为不变。

### D5 — 前端：注册类型 + 补充表单路由（REJECT 跳过）

- `opportunityArtifact.ts`：`ArtifactType` union +6；`ARTIFACT_TYPE_LABELS` +6；`ARTIFACT_LINK_TYPES` +5；
  `STAGE_REQUIRED_ARTIFACTS` +`BIDDING:[BID_DOCUMENT]` +`CONTRACT:[5 类]`；`ADDABLE_ARTIFACT_TYPES` +6（详情可手动添加）。
- `PresaleFlow.requestAdvance`：`STAGE_REQUIRED_ARTIFACTS[stage]` 分支加 `&& decision !== 'REJECT'` 守卫——
  BIDDING/CONTRACT「通过」→ 补充表单（缺则填，齐则直接推进）；「否决」→ 跳过补充 → 确认丢单。POC（decision undefined）不受影响。

## Architecture

```
PresaleFlow [通过 BIDDING/CONTRACT]
  → requestAdvance(r,'PASS')
    → STAGE_REQUIRED_ARTIFACTS[stage] && decision!=='REJECT'
      → 缺失 → 补充表单 → submitSupplement: POST /artifacts ×N (link/report) → advance(id,'PASS')
      → 齐全 → advance(id,'PASS')
PresaleFlow [否决] → requestAdvance(r,'REJECT') → 跳过补充 → confirm → advance(id,'REJECT')

advance(stage=BIDDING,PASS) → persistRequiredArtifact: Path B 校验 BID_DOCUMENT 存在 → next=CONTRACT
advance(stage=CONTRACT,PASS) → persistRequiredArtifact: Path B 校验 5 类存在 → next=INITIATION + WON
advance(stage=BIDDING/CONTRACT,REJECT) → persistRequiredArtifact: gate-reject 跳过 → LOST
```

## Risks / Trade-offs

| 风险 | 缓解 |
|---|---|
| URL 占位非真实附件，用户需自行托管文件 | Gate-1 用户已选；遗留对象存储迁移 |
| 门禁 PASS-only 改动触及既有 advance 路径 | OPPORTUNITY 经 requiredOnReject 保持原状；新增「REJECT 不要求」回归测试；既有 BIDDING-REJECT 测试守护 |
| 既有 `advance_contractPass` 测试会失败 | 预期：更新该测试先 seed 5 件必需产出物 |
| Path A `!isLink` 守卫影响 LEAD/OPPORTUNITY | 二者为 report-kind，`!isLink` 恒真，行为不变 |
| 评审会议纪要选 report 而非 link | 与 DECISION_MINUTES 一致，可系统内编辑+导出；用户「形成纪要」语义契合 |
