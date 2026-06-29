# Test Report: notification-bell (F3, v0.0.102)

## Scope
Frontend-only sub-change (consumes already-shipped A8 backend endpoints). No backend code touched → backend suite not re-run.

## Frontend (vitest --run, full suite)
- Result: **61 files, 299 tests passed (0 failed)**
- Duration: 6.51s
- Delta: 290 → 299 (+9 — 4 NotificationBell + 4 NotificationsPage + 1 carry-over).

## New tests
### `frontend/src/components/NotificationBell.test.tsx`
- TC-NOTIF-BELL-01: bell shows unread badge "3" + opens drawer with 3 rows; api called with `{page:0, size:5, onlyUnread:true}`.
- TC-NOTIF-BELL-02: 全部已读 → markAllNotificationsRead called once → badge disappears after refetch.
- TC-NOTIF-BELL-03: zero unread → no badge + empty drawer state.
- TC-NOTIF-BELL-04: 250 unread → badge text caps at "99+".

### `frontend/src/pages/Notifications/NotificationsPage.test.tsx`
- TC-NOTIF-PAGE-01: 全部 tab renders one unread + one read row; mark button disabled on read row.
- TC-NOTIF-PAGE-02: switching to 未读 tab refetches with `onlyUnread:true`.
- TC-NOTIF-PAGE-03: per-row mark-read calls `markNotificationRead(7)` and refetches; button becomes disabled after refetch.
- TC-NOTIF-PAGE-04: empty list → empty state visible.

## Regression check
- AppLayout test (which now mounts NotificationBell via the shell): still green. NotificationBell tolerates list api rejection (caught + state untouched), so the previously un-mocked `/api/me/notifications` GET in AppLayout/AppRoutes tests is harmless — no test failure observed.
- AppRoutes.test.tsx: all routes still mount; /notifications addition didn't disturb existing assertions.

## Caveats
- No backend changes → no Java/maven test run this batch.
- 「已读」tab is client-filtered (drops `readAt==null` from the page), since backend `onlyUnread` only supports `true|false`; pagination total still reflects the full server-side count — fine for v0.0.102.
- NotificationBell silently swallows api errors (no toast); intentional for a polling widget — surfacing errors is left to /notifications page if needed later.
