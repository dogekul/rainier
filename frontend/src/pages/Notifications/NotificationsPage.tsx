import { useCallback, useEffect, useState } from 'react';
import { Button } from '../../components/ui/Button';
import { Pagination } from '../../components/ui/Pagination';
import {
  listNotifications,
  markNotificationRead,
  NOTIFICATION_LEVEL_LABELS,
  type Notification,
  type NotificationLevel,
} from '../../api/notification';
import './NotificationsPage.css';

type Tab = 'all' | 'unread' | 'read';

const TABS: { value: Tab; label: string }[] = [
  { value: 'all', label: '全部' },
  { value: 'unread', label: '未读' },
  { value: 'read', label: '已读' },
];

const PAGE_SIZE = 20;

/**
 * v0.0.102 (F3) — /notifications 通知中心列表. tabs (全部 / 未读 / 已读) + 分页 + 行内「标记已读」.
 * 「已读」tab 仅本地过滤掉未读行（backend only supports onlyUnread=true|false）；count by tab uses
 * `n.readAt != null`.
 */
export function NotificationsPage() {
  const [tab, setTab] = useState<Tab>('all');
  const [page, setPage] = useState(0);
  const [rows, setRows] = useState<Notification[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState<number | null>(null);

  const load = useCallback(() => {
    setLoading(true);
    return listNotifications({
      page,
      size: PAGE_SIZE,
      onlyUnread: tab === 'unread',
    })
      .then((r) => {
        let content = r.content;
        if (tab === 'read') {
          content = content.filter((n) => n.readAt != null);
        }
        setRows(content);
        setTotal(r.total);
      })
      .catch(() => {
        setRows([]);
        setTotal(0);
      })
      .finally(() => setLoading(false));
  }, [tab, page]);

  useEffect(() => {
    void load();
  }, [load]);

  const onTabChange = (next: Tab) => {
    setPage(0);
    setTab(next);
  };

  const onMarkRead = async (id: number) => {
    setBusyId(id);
    try {
      await markNotificationRead(id);
      await load();
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div className="rainier-page">
      <div className="rainier-page-head">
        <h2>通知中心</h2>
      </div>

      <div className="notif-page-tabs" data-testid="notif-page-tabs">
        {TABS.map((t) => (
          <button
            key={t.value}
            type="button"
            className={`notif-page-tab${tab === t.value ? ' is-active' : ''}`}
            onClick={() => onTabChange(t.value)}
            data-testid={`notif-page-tab-${t.value}`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {loading && rows.length === 0 ? (
        <div className="rainier-empty" data-testid="notif-page-loading">
          加载中…
        </div>
      ) : rows.length === 0 ? (
        <div className="rainier-empty" data-testid="notif-page-empty">
          暂无通知。
        </div>
      ) : (
        <div data-testid="notif-page-list">
          {rows.map((n) => {
            const read = n.readAt != null;
            const level: NotificationLevel = n.level;
            return (
              <div key={n.id} className="notif-page-row" data-testid={`notif-row-${n.id}`}>
                <span className={`rainier-notif-level rainier-notif-level-${level}`}>
                  {NOTIFICATION_LEVEL_LABELS[level]}
                </span>
                <div className="notif-page-row-main">
                  <div className="notif-page-row-title">{n.title}</div>
                  <div className="notif-page-row-body">{n.body}</div>
                  <div className="notif-page-row-meta">
                    {n.createdAt ?? ''}
                    {n.entityType
                      ? ` · ${n.entityType}${n.entityId != null ? '#' + n.entityId : ''}`
                      : ''}
                    {read ? ` · 已读 ${n.readAt}` : ''}
                  </div>
                </div>
                <Button
                  type="button"
                  variant="secondary"
                  disabled={read || busyId === n.id}
                  onClick={() => void onMarkRead(n.id)}
                  data-testid={`notif-row-mark-${n.id}`}
                >
                  {read ? '已读' : '标记已读'}
                </Button>
              </div>
            );
          })}
        </div>
      )}

      <Pagination page={page} size={PAGE_SIZE} total={total} onPageChange={setPage} />
    </div>
  );
}

export default NotificationsPage;
