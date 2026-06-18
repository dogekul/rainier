import client from './client';

/** v0.0.39 — one Story awaiting review by the current user (the「我的评审」queue). */
export interface PendingReview {
  storyId: number;
  code: string;
  title: string;
  status: string;
  priority: string;
  reviewStatus: string;
  projectId: number | null;
  projectName: string | null;
  sprintId: number | null;
  sprintName: string | null;
  ownerUserId: number | null;
  ownerName: string | null;
  ownerLoginName: string | null;
  createTime: string;
}

export type ReviewDecision = 'APPROVED' | 'REJECTED';

/** GET /api/me/pending-reviews — stories where I am the reviewer and review is PENDING (sorted, enriched). */
export async function getPendingReviews(): Promise<PendingReview[]> {
  const res = await client.get<PendingReview[]>('/me/pending-reviews');
  return res.data;
}

/** POST /api/stories/{id}/review — record a one-click decision (APPROVED / REJECTED). */
export async function submitReview(storyId: number, decision: ReviewDecision): Promise<void> {
  await client.post(`/stories/${storyId}/review`, { decision });
}
