# Spec — ai-error-board-ui (A9, v0.0.73)

capability: ai-error-board-ui

## Scenario 1: All users can browse the AI error board
GIVEN a logged-in user (admin or plain) navigates to `/ai/errors`
WHEN the page mounts and `listAiErrors({ size: 100 })` resolves with rows
THEN the page renders a title "AI 错误公示板", a stat-tile summary (OPEN / FIXED counts),
  and one row per error showing `occurredAt`, `aiAction`, `errorDesc`, `affectedEntityType:affectedEntityId`, and a `StatusChip` for `status`.

## Scenario 2: Admin can mark an OPEN error as fixed
GIVEN an admin (a user with `isElevated` true) views the board
AND a row with `status="OPEN"` is present
WHEN the admin clicks 「标记修复」 → the Drawer opens → admin types a `fixAction` → clicks 「确认修复」
THEN `fixAiError(id, fixAction)` is called
AND the list is refetched.

## Scenario 3: Non-admin cannot see the 「标记修复」 button
GIVEN a plain (non-elevated) user views the board
WHEN OPEN rows render
THEN no `「标记修复」` button is shown for any row (read-only board).

## Test cases (vitest, AiErrorsPage.test.tsx)
- TC-AIEP-01: lists errors and renders the OPEN status chip.
- TC-AIEP-02: admin sees the 「标记修复」 button on OPEN rows; submitting calls `fixAiError` and refetches.
- TC-AIEP-03: empty list → empty state shown.
