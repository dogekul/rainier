import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { NotificationBell } from './NotificationBell';
import {
  listNotifications,
  markAllNotificationsRead,
  type Notification,
} from '../api/notification';

vi.mock('../api/notification', async (orig) => ({
  ...(await orig<typeof import('../api/notification')>()),
  listNotifications: vi.fn(),
  markAllNotificationsRead: vi.fn(),
}));

function n(id: number, readAt: string | null = null): Notification {
  return {
    id,
    title: '风险告警 ' + id,
    body: '某任务逾期 3 天',
    level: 'CRIT',
    entityType: 'Task',
    entityId: id,
    createdAt: '2026-06-25T10:00:00',
    readAt,
  };
}

function page(rows: Notification[], total: number = rows.length) {
  return { content: rows, total, page: 0, size: 5 };
}

function renderBell() {
  return render(
    <MemoryRouter>
      <NotificationBell />
    </MemoryRouter>,
  );
}

describe('NotificationBell (F3 TC-NOTIF-BELL)', () => {
  beforeEach(() => {
    vi.mocked(listNotifications).mockReset();
    vi.mocked(markAllNotificationsRead).mockReset();
  });

  /** TC-NOTIF-BELL-01: badge shows the unread count, drawer lists the rows. */
  it('shows the unread badge and the latest unread rows in a Drawer (TC-NOTIF-BELL-01)', async () => {
    vi.mocked(listNotifications).mockResolvedValue(page([n(1), n(2), n(3)], 3));
    renderBell();

    await waitFor(() =>
      expect(screen.getByTestId('notif-bell-badge')).toHaveTextContent('3'),
    );

    fireEvent.click(screen.getByTestId('notif-bell'));
    await waitFor(() => expect(screen.getByTestId('notif-bell-drawer')).toBeInTheDocument());
    expect(screen.getByTestId('notif-bell-row-1')).toBeInTheDocument();
    expect(screen.getByTestId('notif-bell-row-2')).toBeInTheDocument();
    expect(screen.getByTestId('notif-bell-row-3')).toBeInTheDocument();
    expect(listNotifications).toHaveBeenCalledWith({ page: 0, size: 5, onlyUnread: true });
  });

  /** TC-NOTIF-BELL-02: clicking 全部已读 calls markAllNotificationsRead and badge clears. */
  it('marks all unread as read and clears the badge (TC-NOTIF-BELL-02)', async () => {
    vi.mocked(listNotifications)
      .mockResolvedValueOnce(page([n(1), n(2)], 2))
      .mockResolvedValueOnce(page([n(1), n(2)], 2)) // open click refresh
      .mockResolvedValue(page([], 0)); // post-mark refresh
    vi.mocked(markAllNotificationsRead).mockResolvedValue({ updated: 2 });
    renderBell();

    await waitFor(() =>
      expect(screen.getByTestId('notif-bell-badge')).toHaveTextContent('2'),
    );
    fireEvent.click(screen.getByTestId('notif-bell'));
    await waitFor(() => expect(screen.getByTestId('notif-bell-mark-all')).toBeEnabled());

    fireEvent.click(screen.getByTestId('notif-bell-mark-all'));

    await waitFor(() => expect(markAllNotificationsRead).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(screen.queryByTestId('notif-bell-badge')).not.toBeInTheDocument());
  });

  /** TC-NOTIF-BELL-03: zero unread → no badge + empty drawer state. */
  it('renders empty state when no unread notifications (TC-NOTIF-BELL-03)', async () => {
    vi.mocked(listNotifications).mockResolvedValue(page([], 0));
    renderBell();

    await waitFor(() => expect(listNotifications).toHaveBeenCalled());
    expect(screen.queryByTestId('notif-bell-badge')).not.toBeInTheDocument();

    fireEvent.click(screen.getByTestId('notif-bell'));
    await waitFor(() => expect(screen.getByTestId('notif-bell-empty')).toBeInTheDocument());
  });

  /** TC-NOTIF-BELL-04: 100+ unread → badge caps at "99+". */
  it('caps the badge text at 99+ (TC-NOTIF-BELL-04)', async () => {
    vi.mocked(listNotifications).mockResolvedValue(page([n(1)], 250));
    renderBell();
    await waitFor(() =>
      expect(screen.getByTestId('notif-bell-badge')).toHaveTextContent('99+'),
    );
  });
});
