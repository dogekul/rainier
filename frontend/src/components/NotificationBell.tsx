import { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from './ui/Button';
import { Drawer } from './ui/Drawer';
import {
  listNotifications,
  markAllNotificationsRead,
  NOTIFICATION_LEVEL_LABELS,
  type Notification,
} from '../api/notification';
import './NotificationBell.css';

const POLL_MS = 30_000;

/**
 * v0.0.102 (F3) — top-bar 通知铃铛. 30s polls `/api/me/notifications?onlyUnread=true&size=5`,
 * shows the unread count in a red badge (>=99 → "99+"), and opens a right-anchored Drawer with
 * the latest 5 unread rows. Two actions in the Drawer footer: 全部已读 (POST /read-all) and 查看全部
 * (route to /notifications).
 */
export function NotificationBell() {
  const [open, setOpen] = useState(false);
  const [rows, setRows] = useState<Notification[]>([]);
  const [unread, setUnread] = useState(0);
  const [busy, setBusy] = useState(false);
  const navigate = useNavigate();

  const refresh = useCallback(async () => {
    try {
      const res = await listNotifications({ page: 0, size: 5, onlyUnread: true });
      setRows(res.content);
      setUnread(res.total);
    } catch {
      // Network/auth errors are non-fatal for the bell; leave the existing badge alone.
    }
  }, []);

  useEffect(() => {
    void refresh();
    const t = window.setInterval(() => {
      void refresh();
    }, POLL_MS);
    return () => window.clearInterval(t);
  }, [refresh]);

  const badge = unread === 0 ? '' : unread >= 99 ? '99+' : String(unread);

  const onMarkAll = async () => {
    setBusy(true);
    try {
      await markAllNotificationsRead();
      await refresh();
    } finally {
      setBusy(false);
    }
  };

  const onViewAll = () => {
    setOpen(false);
    navigate('/notifications');
  };

  return (
    <>
      <button
        type="button"
        className="rainier-notif-bell"
        onClick={() => {
          setOpen(true);
          void refresh();
        }}
        aria-label="通知"
        data-testid="notif-bell"
      >
        <span aria-hidden="true">🔔</span>
        {badge ? (
          <span className="rainier-notif-bell-badge" data-testid="notif-bell-badge">
            {badge}
          </span>
        ) : null}
      </button>
      <Drawer
        open={open}
        title="最新通知"
        onClose={() => setOpen(false)}
        footer={
          <>
            <Button
              type="button"
              variant="secondary"
              onClick={() => void onMarkAll()}
              disabled={busy || unread === 0}
              data-testid="notif-bell-mark-all"
            >
              全部已读
            </Button>
            <Button
              type="button"
              variant="primary"
              onClick={onViewAll}
              data-testid="notif-bell-view-all"
            >
              查看全部
            </Button>
          </>
        }
      >
        <div data-testid="notif-bell-drawer">
          {rows.length === 0 ? (
            <div className="rainier-notif-drawer-empty" data-testid="notif-bell-empty">
              暂无未读通知。
            </div>
          ) : (
            rows.map((n) => (
              <div
                key={n.id}
                className="rainier-notif-drawer-row"
                data-testid={`notif-bell-row-${n.id}`}
              >
                <div className="rainier-notif-drawer-row-head">
                  <span className={`rainier-notif-level rainier-notif-level-${n.level}`}>
                    {NOTIFICATION_LEVEL_LABELS[n.level]}
                  </span>
                  <span>{n.title}</span>
                </div>
                <div className="rainier-notif-drawer-row-body">{n.body}</div>
                <div className="rainier-notif-drawer-row-meta">
                  {n.createdAt ?? ''}
                  {n.entityType
                    ? ` · ${n.entityType}${n.entityId != null ? '#' + n.entityId : ''}`
                    : ''}
                </div>
              </div>
            ))
          )}
        </div>
      </Drawer>
    </>
  );
}
