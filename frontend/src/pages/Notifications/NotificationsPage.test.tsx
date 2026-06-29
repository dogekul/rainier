import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { NotificationsPage } from './NotificationsPage';
import {
  listNotifications,
  markNotificationRead,
  type Notification,
} from '../../api/notification';

vi.mock('../../api/notification', async (orig) => ({
  ...(await orig<typeof import('../../api/notification')>()),
  listNotifications: vi.fn(),
  markNotificationRead: vi.fn(),
}));

function n(id: number, opts: Partial<Notification> = {}): Notification {
  return {
    id,
    title: `通知 ${id}`,
    body: '某事件已发生',
    level: 'INFO',
    entityType: 'Task',
    entityId: id,
    createdAt: '2026-06-25T09:00:00',
    readAt: null,
    ...opts,
  };
}

function page(rows: Notification[], total = rows.length, p = 0, size = 20) {
  return { content: rows, total, page: p, size };
}

function renderPage() {
  return render(
    <MemoryRouter>
      <NotificationsPage />
    </MemoryRouter>,
  );
}

describe('NotificationsPage (F3 TC-NOTIF-PAGE)', () => {
  beforeEach(() => {
    vi.mocked(listNotifications).mockReset();
    vi.mocked(markNotificationRead).mockReset();
  });

  /** TC-NOTIF-PAGE-01: 默认渲染列表 + tabs + 每行带「标记已读」按钮，已读行 disable. */
  it('renders rows with per-row mark-read button; read row disabled (TC-NOTIF-PAGE-01)', async () => {
    vi.mocked(listNotifications).mockResolvedValue(
      page([n(1), n(2, { readAt: '2026-06-25T11:00:00' })], 2),
    );
    renderPage();

    await waitFor(() => expect(screen.getByTestId('notif-row-1')).toBeInTheDocument());
    expect(screen.getByText('通知中心')).toBeInTheDocument();
    expect(screen.getByTestId('notif-row-2')).toBeInTheDocument();

    expect(screen.getByTestId('notif-row-mark-1')).toBeEnabled();
    expect(screen.getByTestId('notif-row-mark-2')).toBeDisabled();
  });

  /** TC-NOTIF-PAGE-02: switching to 未读 tab refetches with onlyUnread=true. */
  it('switches tab and refetches with onlyUnread=true (TC-NOTIF-PAGE-02)', async () => {
    vi.mocked(listNotifications).mockResolvedValue(page([n(1)], 1));
    renderPage();

    await waitFor(() => expect(screen.getByTestId('notif-row-1')).toBeInTheDocument());
    expect(listNotifications).toHaveBeenLastCalledWith({
      page: 0,
      size: 20,
      onlyUnread: false,
    });

    fireEvent.click(screen.getByTestId('notif-page-tab-unread'));
    await waitFor(() =>
      expect(listNotifications).toHaveBeenLastCalledWith({
        page: 0,
        size: 20,
        onlyUnread: true,
      }),
    );
  });

  /** TC-NOTIF-PAGE-03: per-row mark-read calls api + refetches. */
  it('marks a single row as read and refetches (TC-NOTIF-PAGE-03)', async () => {
    vi.mocked(listNotifications)
      .mockResolvedValueOnce(page([n(7)], 1))
      .mockResolvedValueOnce(page([n(7, { readAt: '2026-06-25T12:00:00' })], 1));
    vi.mocked(markNotificationRead).mockResolvedValue(
      n(7, { readAt: '2026-06-25T12:00:00' }),
    );
    renderPage();

    await waitFor(() => expect(screen.getByTestId('notif-row-mark-7')).toBeEnabled());
    fireEvent.click(screen.getByTestId('notif-row-mark-7'));

    await waitFor(() => expect(markNotificationRead).toHaveBeenCalledWith(7));
    await waitFor(() => expect(screen.getByTestId('notif-row-mark-7')).toBeDisabled());
  });

  /** TC-NOTIF-PAGE-04: empty list → empty state. */
  it('renders empty state when no notifications (TC-NOTIF-PAGE-04)', async () => {
    vi.mocked(listNotifications).mockResolvedValue(page([], 0));
    renderPage();
    await waitFor(() => expect(screen.getByTestId('notif-page-empty')).toBeInTheDocument());
  });
});
