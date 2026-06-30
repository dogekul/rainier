# Risk Scan Dedup Scheduler

## Problem

`RiskService.runAll` pushes CRIT findings into the in-app notification table, but repeated scans can create duplicate unread notifications for the same unresolved risk. There is also no periodic scan entry point, so risk push remains mostly user-driven.

## Scope

- Suppress duplicate unread CRIT notifications for the same user/rule/entity/title within a configurable window.
- Add a lightweight scheduled scanner that runs `RiskService.runAll` for enabled users.
- Keep tests deterministic by disabling scheduled scans in the default test profile.

## Out Of Scope

- Persisting risk findings as a first-class table.
- IM/email escalation for stale risks.
- Organization-wide scan scopes or per-role scan policies.
