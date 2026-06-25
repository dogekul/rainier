import client from './client';

/** v0.0.93 (D5) — one overdue project row in the CRM metrics snapshot. */
export interface OverdueProjectRow {
  projectId: number;
  code: string;
  name: string;
  status: string;
  ownerUserId: number | null;
  expectedEndDate: string | null;
  daysOverdue: number;
}

/** v0.0.93 (D5) — composite CRM/delivery health snapshot. All rate fields nullable when no samples. */
export interface CrmMetrics {
  winRate: number | null;
  dealRate: number | null;
  avgDeliveryCycleDays: number | null;
  overdueProjects: OverdueProjectRow[];
}

export interface CrmMetricsQuery {
  periodStart?: string; // ISO-8601 Instant
  periodEnd?: string;
  ownerUserId?: number;
  scope?: string; // forward-compat, currently ignored server-side
}

/** GET /api/metrics/crm — composite snapshot for the 度量看板 page. */
export async function getCrmMetrics(q: CrmMetricsQuery = {}): Promise<CrmMetrics> {
  const params: Record<string, string | number> = {};
  if (q.periodStart) params.periodStart = q.periodStart;
  if (q.periodEnd) params.periodEnd = q.periodEnd;
  if (q.ownerUserId != null) params.ownerUserId = q.ownerUserId;
  if (q.scope) params.scope = q.scope;
  const res = await client.get<CrmMetrics>('/metrics/crm', { params });
  return res.data;
}
