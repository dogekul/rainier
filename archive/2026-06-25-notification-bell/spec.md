# Spec: notification-bell (capability=notification-bell)

## Scenario 1: bell shows unread badge and opens Drawer with latest 5 unread
GIVEN the current user has 3 unread notifications returned by `GET /api/me/notifications?onlyUnread=true&page=0&size=5`
WHEN `<NotificationBell />` mounts inside the AppLayout
THEN the bell's `data-testid="notif-bell-badge"` shows "3"
AND clicking the bell opens a Drawer (`data-testid="notif-bell-drawer"`)
AND the Drawer body lists 3 rows (each `data-testid="notif-bell-row-<id>"`) with title + body + level chip

## Scenario 2: markAllRead clears the badge
GIVEN the bell shows badge "3" with the Drawer open
WHEN the user clicks `data-testid="notif-bell-mark-all"`
THEN `POST /api/me/notifications/read-all` is called
AND a refetch returns 0 unread, the badge disappears (or shows nothing)

## Scenario 3: /notifications page lists rows, supports tab filter and per-row mark-read
GIVEN the user navigates to `/notifications`
AND the backend returns `{content: [n1 unread, n2 read], total: 2, page: 0, size: 20}`
WHEN the page renders
THEN both rows are visible (`data-testid="notif-row-<id>"`) with level / title / body / createdAt / source
AND switching to the "未读" tab refetches with `onlyUnread=true`
AND clicking `data-testid="notif-row-mark-<id>"` on the unread row calls `POST /api/me/notifications/<id>/read` and refetches
AND the already-read row's mark button is disabled
