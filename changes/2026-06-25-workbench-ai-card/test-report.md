# Test Report: workbench-ai-card (F4, v0.0.103)

## Scope
Frontend-only change. No backend code touched, so backend tests not re-run (already green at v0.0.102).

## Frontend
`cd frontend && npm test -- --run`

- Test files: 62 passed
- Tests: 304 passed (previously 290 baseline + 9 new from this batch + 5 from earlier F sub-changes already counted)
- Duration: ~5.6s

### New tests
- `frontend/src/components/AiSuggestionCard.test.tsx` (4 cases)
  - TC-AI-CARD-01: renders ≤3 PROPOSED rows + parses `evidence.eventId/source` back-pointer; non-JSON evidence silently skipped
  - TC-AI-CARD-02: 采纳 flips row to 已采纳 + 撤销 button; 撤销 calls `/reverse` and refreshes
  - TC-AI-CARD-03: 驳回 inline form: empty reason → inline error; valid reason → `decideAiWorkLog(id, REJECTED, reason)` + refresh
  - TC-AI-CARD-04: empty list shows `ai-suggest-empty` hint
- `frontend/src/pages/Workbench/WorkbenchPage.test.tsx` (+1 case)
  - TC-WB-AI-CARD: WorkbenchPage embeds `<AiSuggestionCard />` and the seeded PROPOSED row renders

## Caveats
- No per-user filter on the list call (AiWorkLog has no `targetOwnerUserId` field yet); the card shows the freshest PROPOSED rows globally. Out of scope, documented in proposal.md.
- Undo window is a soft 5-second client check via `Date.now()`; no animated countdown. Past 5s the button is hidden (Card re-render). The backend `/reverse` still enforces the real eligibility.
- evidence schema is best-effort parsed for `{eventId, source}`; richer rendering is a future change.
