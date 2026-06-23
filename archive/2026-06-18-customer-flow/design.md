# Design — v0.0.44-customer-flow

> Baseline: tag `v0.0.43-ai-work-log` / commit 0666a61. Gate 1 model locked (multi-entity / 4 owners /
> WON@contract / amount / delivery=Project(B)).

## Context

The「客户全流程管理」spans 售前→实施→售后. **理顺 (2026-06-22):** Opportunity now carries the full
售前(5)+实施(5)=10-node journey faithful to the screenshot (was 5 售前 nodes), with the 实施 *work* still
running in a linked Project (立项移交). Operation covers 售后 (3 nodes). Mirrors the established stage-machine
pattern (Story review, AiWorkLog decision) + entity CRUD + board landing pages. ddl-auto auto-creates the
2 new tables (21→23).

## Decisions

### D1 — Opportunity (客户全流程): 10-node pipeline + 4 gates + WON/LOST
**理顺 (2026-06-22):** faithful 还原客户全流程图的两段泳道。Table `rainier_opportunity` extends BaseEntity
(soft-delete). Stages (`STAGE_ORDER`, 10 节点) =
**售前环节** `LEAD → OPPORTUNITY → POC → BIDDING → CONTRACT` then
**实施环节** `INITIATION → SURVEY → REQUIREMENT → DELIVERY → ACCEPTANCE`.
`GATE_STAGES = {OPPORTUNITY, BIDDING, CONTRACT, INITIATION}` (商机/投标/合同/立项决策);
`PRESALES_GATES = {OPPORTUNITY, BIDDING, CONTRACT}`. Status `OPEN/WON/LOST` (default OPEN).
- `advance {decision?, note?}`: blocked when status==LOST (409) or already at ACCEPTANCE (terminal, 409).
  From a non-gate stage → next stage. From a gate stage → `decision` required (∈{PASS,REJECT}, else 400),
  records gateDecidedBy.
  - **售前关口** REJECT → status=LOST (丢单, terminal). **立项关口 (INITIATION)** REJECT → 停在「立项」，
    status 不变（可重审）—— 立项被否不等于丢单。
  - PASS advances to the next node. PASS **from CONTRACT** → status=WON and stage=INITIATION (赢单=合同签订，
    进入实施环节首节点「立项」).
- Why WON at CONTRACT: a signed contract IS the win; 立项→验收 are post-win 实施 execution. A WON opp keeps
  advancing through 实施 (WON ≠ flow end); only LOST / 验收 are terminal.

### D2 — Opportunity → Project handoff (立项移交)
`initiate {projectId, decision, note?}`: only when status==WON (else 409); `decision`∈{PASS,REJECT} (else 400);
`projectId` 须存在 (else 400). PASS → sets `projectId` (links the delivery Project) — the 实施 work runs in that
Project (unchanged). REJECT → records the rejection (projectId stays null). This is the explicit 立项移交 entry;
it coexists with the INITIATION advance-gate (advance walks the stage chain; initiate links the Project). 实施
itself adds no new code (B).

### D3 — Operation (售后): linear 3-stage pipeline
Table `rainier_operation`. Stages `MAINTENANCE(回款/维保) → OPERATING(运营) → REPURCHASE(复购)`. Status
`ACTIVE/CLOSED`. `advance {}` → next stage (no gates); at the last stage, advance → status=CLOSED.
`opsOwnerUserId` + `projectId` (link to the delivered Project) nullable. Created after 验收 (manually /
linked).

### D4 — 4 owners + amount + validation
Opportunity carries 4 nullable owner FKs (commercial/solution/pm/ops), each `existsById`-validated when
non-null (the StoryService owner-validation pattern). `amount` is a nullable BigDecimal/long (deal value)
— powers pipeline value (各节点在谈金额) + 赢单金额. Stored as a plain numeric column.

### D5 — DTOs + service (mirror Story/AiWorkLog)
Per entity: `*Detail` (from entity + owner-name enrichment for opportunity), `*CreateRequest`,
`*UpdateRequest`, plus `OpportunityAdvanceRequest{decision,note}` / `OpportunityInitiateRequest{projectId,
decision,note}` / `OperationAdvanceRequest{}`. Services validate stage/status/owners; list via
JpaSpecificationExecutor + PageResponse.

### D6 — frontend 客户 nav group
NEW top-level「客户」navGroup (all-users, no requiresAdmin; /crm/* NOT in ADMIN_PATH_PREFIXES →
navGuardConsistency auto-pins all-users). 4 items: 商机看板 / 售前流转 / 实施流转 / 运营看板. `OperationBoard`
/crm/operations: 3 stage columns + new + advance (售后, unchanged). Reuse board-kit.

### D7 — 看板/流转分离（2026-06-23 修订）
**理由**：用户要求「商机看板只做看板（给某监控角色查看所有商机进展），流转单独做操作页，并把售前/实施区分开」。看板
混入操作按钮违背单一职责，且监控视角与操作视角的受众不同。**拆为三页（前端重构，后端不变）**：

- **`OpportunityBoard` /crm/opportunities —「商机看板」(READ-ONLY)**：两段泳道 + 10 节点分列 + 只读卡片（客户/标题/
  金额/负责人/赢单标识）+ StatTiles。**零操作控件**（无新建/推进/关口）。`listOpportunities({size:100})`. 受众=监控
  角色（待定，暂 all-users）。`opp-readonly-hint` 标注「操作请到 售前/实施 流转」。
- **`PresaleFlow` /crm/presale-flow —「售前流转」(操作页)**：列出 `status=OPEN ∧ stage∈OPP_PRESALE_STAGES` 的商机
  （`rainier-list-table` 行 = 阶段 chip / 客户·标题 / 金额 / 负责人 / 操作）。操作：非关口→推进；关口(商机/投标/合同)→
  通过(PASS)/否决(REJECT→丢单)。**新建商机** drawer（客户/标题/金额/四负责人 + 校验）移到此页（看板不再建单）。
- **`DeliveryFlow` /crm/delivery-flow —「实施流转」(操作页)**：列出 `status=WON ∧ stage∈OPP_DELIVERY_STAGES` 的商机
  （行 = 阶段 / 客户·标题 / 项目经理 / 关联Project / 操作）。操作：立项(INITIATION)→**立项移交**(drawer 选 Project →
  `initiate(id,projectId,'PASS')`，链入交付 Project，**补齐 v0.0.44 遗留的 initiate UI**) + 通过/否决；非关口→推进；
  验收(ACCEPTANCE)→终态「已验收」无操作。

`api/opportunity.ts` 加 `OPP_PRESALE_STAGES`/`OPP_DELIVERY_STAGES`（派生自 OPP_PHASES）。后端 advance/initiate/list
端点零改。`OperationBoard`(运营/售后) 不动。

## Architecture / Data flow

```
客户全流程 Opportunity (one entity, 10 nodes, two bands):
 售前环节: LEAD→OPPORTUNITY→[商机决策]→POC→BIDDING→[投标决策]→CONTRACT→[合同评审 PASS ⇒ WON]
            售前关口 REJECT → LOST (丢单, terminal)
 实施环节: INITIATION→[立项评审]→SURVEY→REQUIREMENT→DELIVERY→ACCEPTANCE (验收, terminal)
            立项评审 REJECT → 停在 INITIATION (可重审, 不丢单)
立项移交: WON Opportunity --initiate(projectId, PASS)--> linked delivery Project (实施 work = existing Project)
售后: Operation   MAINTENANCE→OPERATING→REPURCHASE→CLOSED   (projectId links the delivered Project)
boards: /crm/opportunities (售前/实施 两段泳道), /crm/operations (客户 nav group, all-users)
```

## Risks / Trade-offs

| Risk | Mitigation |
|---|---|
| Large slice (2 entities) | bounded: CRUD + advance + gates only; 度量/活动/输出物/auto-spawn are follow-ups |
| 立项评审 home (实施=Project has no stage machine) | folded into Opportunity.initiate handoff; full 实施 stage tracking is a follow-up |
| stage machine edge cases | advance guarded by status==OPEN + STAGE_ORDER bounds; WON/LOST terminal; tests cover gates |
| new tables | ddl-auto; table-count test 21→23 |
| 存量数据 | additive new tables; Project unchanged; standing 约束 honored |
| Java 8 | constants via unmodifiableSet/List + Arrays.asList; no Set.of/var/no-arg orElseThrow; temurin-8 gate |
