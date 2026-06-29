import client from './client';
import type { PaginatedResult } from '../hooks/usePaginated';

export type AiWorkLogStatus = 'PROPOSED' | 'ACCEPTED' | 'REJECTED';
export type AiDecision = 'ACCEPTED' | 'REJECTED';

/** v0.0.43 — an AI agent's proposed action (flywheel base). */
export interface AiWorkLog {
  id: number;
  agentType: string;
  action: string;
  targetType?: string | null;
  targetId?: number | null;
  summary: string;
  evidence: string;
  status: AiWorkLogStatus;
  decidedBy?: string | null;
  decidedAt?: string | null;
  rejectReason?: string | null;
  createTime?: string;
}

export const AI_STATUS_LABELS: Record<AiWorkLogStatus, string> = {
  PROPOSED: '待裁决',
  ACCEPTED: '已采纳',
  REJECTED: '已驳回',
};

export interface AiWorkLogListParams {
  agentType?: string;
  status?: AiWorkLogStatus;
  page?: number;
  size?: number;
}

/** GET /api/ai-work-logs — paginated AI proposals, newest first. */
export async function listAiWorkLogs(
  params: AiWorkLogListParams = {},
): Promise<PaginatedResult<AiWorkLog>> {
  const res = await client.get<PaginatedResult<AiWorkLog>>('/ai-work-logs', { params });
  return res.data;
}

/** POST /api/ai-work-logs/{id}/decision — accept/reject a PROPOSED proposal (reject needs a reason). */
export async function decideAiWorkLog(
  id: number,
  decision: AiDecision,
  reason?: string,
): Promise<AiWorkLog> {
  const res = await client.post<AiWorkLog>(`/ai-work-logs/${id}/decision`, { decision, reason });
  return res.data;
}

/**
 * F4 (v0.0.103) — list the latest PROPOSED suggestions for the workbench card. No per-user
 * filter yet (AiWorkLog has no targetOwnerUserId), so we just take the freshest size=3.
 */
export async function listMyProposals(size: number = 3): Promise<AiWorkLog[]> {
  const res = await client.get<PaginatedResult<AiWorkLog>>('/ai-work-logs', {
    params: { status: 'PROPOSED', page: 0, size },
  });
  return res.data.content;
}

/** F4 sugar — `decideAiWorkLog(id, 'ACCEPTED')`. */
export function acceptWorkLog(id: number): Promise<AiWorkLog> {
  return decideAiWorkLog(id, 'ACCEPTED');
}

/** F4 sugar — `decideAiWorkLog(id, 'REJECTED', reason)`. */
export function rejectWorkLog(id: number, reason: string): Promise<AiWorkLog> {
  return decideAiWorkLog(id, 'REJECTED', reason);
}

/**
 * F4 — undo a previously ACCEPTED log via the F1 backend endpoint. 400 when the log is no longer
 * ACCEPTED or its reverseSnapshot has been cleared.
 */
export async function reverseWorkLog(id: number): Promise<AiWorkLog> {
  const res = await client.post<AiWorkLog>(`/ai-work-logs/${id}/reverse`);
  return res.data;
}
