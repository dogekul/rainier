import client from './client';

/**
 * v0.0.39 / v0.0.82 — one item awaiting review by the current user. Backed by GET
 * /api/me/pending-reviews. `kind` discriminates Story vs Task rows; the corresponding id field
 * (storyId / taskId) is populated based on kind.
 */
export interface PendingReview {
  /** v0.0.82: "STORY" | "TASK". Optional for legacy clients. */
  kind?: 'STORY' | 'TASK';
  storyId: number | null;
  /** v0.0.82: filled when kind === "TASK". */
  taskId?: number | null;
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

/** GET /api/me/pending-reviews — merged Story + Task items pending review by current user. */
export async function getPendingReviews(): Promise<PendingReview[]> {
  const res = await client.get<PendingReview[]>('/me/pending-reviews');
  return res.data;
}

/** POST /api/stories/{id}/review — record a one-click decision (APPROVED / REJECTED). */
export async function submitReview(storyId: number, decision: ReviewDecision): Promise<void> {
  await client.post(`/stories/${storyId}/review`, { decision });
}

/**
 * v0.0.82: POST /api/tasks/{id}/review. {@code reason} required for REJECTED (≤500 chars).
 */
export async function submitTaskReview(
  taskId: number,
  decision: ReviewDecision,
  reason?: string,
): Promise<void> {
  await client.post(`/tasks/${taskId}/review`, { decision, reason });
}

/**
 * v0.0.112 (H5) — counters powering the 架构师 landing page. {@code approvedThisWeek /
 * rejectedThisWeek} use Story/Task {@code updateTime} as a proxy for {@code reviewedAt}
 * (current ISO week, Monday 00:00 UTC).
 */
export interface ReviewStats {
  pendingStoryCount: number;
  pendingTaskCount: number;
  approvedThisWeek: number;
  rejectedThisWeek: number;
}

/** GET /api/me/review-stats — counters for the 架构师 landing page. */
export async function getReviewStats(): Promise<ReviewStats> {
  const res = await client.get<ReviewStats>('/me/review-stats');
  return res.data;
}
