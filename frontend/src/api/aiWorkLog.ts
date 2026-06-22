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
