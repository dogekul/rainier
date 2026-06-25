# Capability: event-adapters

> NEW capability (v0.0.66-event-adapters, 2026-06-25) — **5 路 EventExtractor stub**。
> 对 v0.0.65 的事件管线提供 GitLab / 钉钉 / 飞书 / 邮件 / 禅道 5 个 source-specific
> extractor bean；GitLab/Zentao 用简单正则抽出内部实体 ref，其余仅 stub（supports
> 命中但 extract empty）。**仍 0 真实集成 — 无 webhook、无签名验证、无状态回写**。

## ADDED Requirements

### Requirement: GitLab adapter 抽取 task ref

GitLab `EventExtractor` SHALL `supports(event)` 当 `event.sourceType="GITLAB"`；
若 `event.payload` 含 `RA-<id>` 字样，`extract` SHALL 返回 `{TASK, <id>, "COMMIT_REF"}`，
否则返回 empty。

#### Scenario: payload 含 RA-123 → TASK 123

- **GIVEN** event sourceType="GITLAB", payload="fix login RA-123 done"
- **WHEN** GitLabAdapter.extract(event)
- **THEN** 返回 Optional 含 `{entityType:"TASK", entityId:123, action:"COMMIT_REF"}`

### Requirement: Zentao adapter 抽取 story ref

Zentao `EventExtractor` SHALL `supports(event)` 当 `event.sourceType="ZENTAO"`；
若 `event.payload` 含 `bug-<id>` 字样，`extract` SHALL 返回 `{STORY, <id>, "BUG_REPORT"}`，
否则返回 empty。

#### Scenario: payload 含 bug-7 → STORY 7

- **GIVEN** event sourceType="ZENTAO", payload="reopen bug-7 reason: ..."
- **WHEN** ZentaoAdapter.extract(event)
- **THEN** 返回 Optional 含 `{entityType:"STORY", entityId:7, action:"BUG_REPORT"}`

### Requirement: DingTalk adapter stub

DingTalk `EventExtractor` SHALL `supports(event)` 当 `event.sourceType="DINGTALK"`；
`extract` SHALL 返回 empty（A2 不做语义抽取）。

#### Scenario: DingTalk event 仅标 processed

- **GIVEN** 一条 DINGTALK 未处理事件
- **WHEN** EventService.process(10)
- **THEN** event.processed SHALL 为 true
- **AND** event.extractedEntityType SHALL 为 null

### Requirement: Feishu adapter stub

Feishu `EventExtractor` SHALL `supports(event)` 当 `event.sourceType="FEISHU"`；
`extract` SHALL 返回 empty。

#### Scenario: Feishu event 仅标 processed

- **GIVEN** 一条 FEISHU 未处理事件
- **WHEN** EventService.process(10)
- **THEN** event.processed SHALL 为 true

### Requirement: Email adapter stub

Email `EventExtractor` SHALL `supports(event)` 当 `event.sourceType="EMAIL"`；
`extract` SHALL 返回 empty。

#### Scenario: Email event 仅标 processed

- **GIVEN** 一条 EMAIL 未处理事件
- **WHEN** EventService.process(10)
- **THEN** event.processed SHALL 为 true
