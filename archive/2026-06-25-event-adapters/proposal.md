# A2: event-adapters (5 个 stub EventExtractor)

## What
为 v0.0.65 飞轮事件管线补齐 5 个 source-specific 的 `EventExtractor` stub bean：
GitLab / 钉钉 / 飞书 / 邮件 / 禅道。其中 GitLab/Zentao 含简易正则抽取，其余仅 stub
（supports 命中但 extract 返回 empty，用于把事件标 processed=true）。

## Why
A1 只搭好契约 + service pipeline；没有任何 production `EventExtractor` bean。A2 把 5 路源接入
（仍 0 真实集成 — 没有 webhook controller、没有签名校验），同时通过 `EventSeed`
在 dev 启动时塞 5 条样例事件，让管线可端到端 demo。

## Scope
- NEW `com.rainier.event.adapter.{GitLab,DingTalk,Feishu,Email,Zentao}Adapter` (5 个 `@Component`)
- NEW `com.rainier.event.bootstrap.EventSeed`（flag `app.demo.event-seed.enabled`，dev=true，test=false）
- application.yml 增 `app.demo.event-seed.enabled: true`
- application-test.yml 增 `app.demo.event-seed.enabled: false`
- NEW spec.md（capability=event-adapters，5 Scenario）
- 测试：GitLab/Zentao adapter unit + 其他 3 adapter supports 单测 + EventService 集成测试（5 event → 全 processed）

## OutOfScope
- 真实 HTTP webhook / 签名验证
- AI 语义推断 / LLM 调用
- 状态变更回写业务实体（留给 A3）
- 前端

## Decisions
- GitLab 正则：`RA-(\d+)` → TASK / COMMIT_REF
- Zentao 正则：`bug-(\d+)` → STORY / BUG_REPORT
- 仅匹配 payload；sourceId 暂不参与抽取
- stub 三家（钉钉/飞书/邮件）supports=true 但 extract 返回 empty —
  EventService 仍把事件标 processed=true，符合 A1 的 "no-match 也置 processed" 约定
