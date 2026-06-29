import client from './client';

/** v0.0.111 (H4) — a direct subordinate of the caller (active member of an org the caller HEADs). */
export interface Subordinate {
  id: number;
  loginName: string | null;
  displayName: string | null;
  primaryOrgName: string | null;
  contributionSummary: {
    weeklyTasksDone: number;
    totalTasks: number;
  };
}

/** GET /api/me/subordinates — direct subordinates of the caller; non-HEAD callers get `[]`. */
export async function listSubordinates(): Promise<Subordinate[]> {
  const res = await client.get<Subordinate[]>('/me/subordinates');
  return res.data;
}
