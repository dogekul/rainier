import client from './client';
import type { PaginatedResult } from '../hooks/usePaginated';

/** v0.0.73 (A9) — surfaced AI mistakes (status board). */
export type AiErrorStatus = 'OPEN' | 'FIXED';

/** v0.0.73 — one row in the AI error board (mirrors backend AiErrorDetail). */
export interface AiError {
  id: number;
  occurredAt: string;
  aiAction: string;
  errorDesc: string;
  affectedEntityType?: string | null;
  affectedEntityId?: number | null;
  rootCause?: string | null;
  status: AiErrorStatus;
  fixAction?: string | null;
  evidence?: string | null;
  createTime?: string;
}

export const AI_ERROR_STATUS_LABELS: Record<AiErrorStatus, string> = {
  OPEN: '待修复',
  FIXED: '已修复',
};

export interface AiErrorListParams {
  status?: AiErrorStatus;
  page?: number;
  size?: number;
}

/** GET /api/ai/errors — paginated AI errors, newest first. all-users readable. */
export async function listAiErrors(
  params: AiErrorListParams = {},
): Promise<PaginatedResult<AiError>> {
  const res = await client.get<PaginatedResult<AiError>>('/ai/errors', { params });
  return res.data;
}

/** POST /api/ai/errors/{id}/fix — mark OPEN error as FIXED (admin only; AdminPaths TIER_B). */
export async function fixAiError(id: number, fixAction: string): Promise<AiError> {
  const res = await client.post<AiError>(`/ai/errors/${id}/fix`, { fixAction });
  return res.data;
}

/** v0.0.104 (F5) — GET /api/ai/errors/overdue-count payload shape. */
export interface AiErrorOverdueCount {
  count: number;
  thresholdHours: number;
}

/**
 * F5 — GET /api/ai/errors/overdue-count?hours=N. all-users readable (Tier B GET passthrough).
 * Drives the global AppLayout overdue banner.
 */
export async function fetchAiErrorOverdueCount(hours: number = 24): Promise<AiErrorOverdueCount> {
  const res = await client.get<AiErrorOverdueCount>('/ai/errors/overdue-count', {
    params: { hours },
  });
  return res.data;
}
