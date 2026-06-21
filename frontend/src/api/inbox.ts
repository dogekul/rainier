import client from './client';
import type { Priority } from './demand';
import type { RequirementStatus } from './requirement';

/** v0.0.42 — an unconverted demand awaiting PO triage. */
export interface InboxDemand {
  id: number;
  title: string;
  priority: Priority;
  status: string;
  createTime?: string;
}

/** A requirement owned by the current user. */
export interface InboxRequirement {
  id: number;
  code: string;
  title: string;
  status: RequirementStatus;
  priority: Priority;
  expectedDate?: string | null;
  projectId?: number | null;
  projectName?: string | null;
}

/** GET /api/me/inbox payload — the PO 需求收件箱. */
export interface Inbox {
  unconvertedDemands: InboxDemand[];
  myRequirements: InboxRequirement[];
}

/** GET /api/me/inbox — unconverted demands (triage) + my requirements. */
export async function getInbox(): Promise<Inbox> {
  const res = await client.get<Inbox>('/me/inbox');
  return res.data;
}
