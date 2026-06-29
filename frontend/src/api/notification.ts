import client from './client';
import type { PaginatedResult } from '../hooks/usePaginated';

/** v0.0.102 (F3) — notification severity tier. */
export type NotificationLevel = 'INFO' | 'WARN' | 'CRIT';

/** v0.0.102 — single notification row (mirrors backend NotificationDetail, v0.0.72/A8). */
export interface Notification {
  id: number;
  userId?: number | null;
  title: string;
  body: string;
  level: NotificationLevel;
  entityType?: string | null;
  entityId?: number | null;
  createdAt?: string;
  readAt?: string | null;
}

export const NOTIFICATION_LEVEL_LABELS: Record<NotificationLevel, string> = {
  INFO: '通知',
  WARN: '告警',
  CRIT: '紧急',
};

export interface ListNotificationParams {
  page?: number;
  size?: number;
  onlyUnread?: boolean;
}

/**
 * GET /api/me/notifications — paginated current-user notifications, newest first.
 * All-users token-gated (backend MeNotificationsController).
 */
export async function listNotifications(
  params: ListNotificationParams = {},
): Promise<PaginatedResult<Notification>> {
  const res = await client.get<PaginatedResult<Notification>>('/me/notifications', {
    params: {
      page: params.page ?? 0,
      size: params.size ?? 20,
      onlyUnread: params.onlyUnread ?? false,
    },
  });
  return res.data;
}

/** POST /api/me/notifications/{id}/read — flip a single notification to read. */
export async function markNotificationRead(id: number): Promise<Notification> {
  const res = await client.post<Notification>(`/me/notifications/${id}/read`);
  return res.data;
}

/** POST /api/me/notifications/read-all — bulk mark every unread notification as read. */
export async function markAllNotificationsRead(): Promise<{ updated: number }> {
  const res = await client.post<{ updated: number }>('/me/notifications/read-all');
  return res.data;
}
