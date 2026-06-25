import client from './client';
import type { OpportunityArtifact } from './opportunityArtifact';

/** v0.0.90 D2 — 商机 stage 活动清单 + dashboard 整合视图 API. */

export type StageActivityStatus = 'PENDING' | 'DONE' | 'SKIPPED';

export interface StageActivity {
  id: number;
  opportunityId: number;
  stageCode: string;
  activityTitle: string;
  description?: string | null;
  assigneeUserId?: number | null;
  dueDate?: string | null;
  status: StageActivityStatus;
  completedAt?: string | null;
  createBy?: string | null;
  createTime?: string;
}

export interface StageActivityCreate {
  activityTitle: string;
  description?: string;
  assigneeUserId?: number;
  dueDate?: string;
}

export interface StageDashboardView {
  opportunityId: number;
  stageCode: string;
  activities: StageActivity[];
  artifacts: OpportunityArtifact[];
}

export async function listStageActivities(
  opportunityId: number,
  stageCode: string,
): Promise<StageActivity[]> {
  const res = await client.get<StageActivity[]>(
    `/opportunities/${opportunityId}/stages/${stageCode}/activities`,
  );
  return res.data;
}

export async function addStageActivity(
  opportunityId: number,
  stageCode: string,
  body: StageActivityCreate,
): Promise<StageActivity> {
  const res = await client.post<StageActivity>(
    `/opportunities/${opportunityId}/stages/${stageCode}/activities`,
    body,
  );
  return res.data;
}

export async function markStageActivityDone(activityId: number): Promise<StageActivity> {
  const res = await client.post<StageActivity>(`/stage-activities/${activityId}/done`);
  return res.data;
}

export async function skipStageActivity(activityId: number): Promise<StageActivity> {
  const res = await client.post<StageActivity>(`/stage-activities/${activityId}/skip`);
  return res.data;
}

export async function getStageDashboard(
  opportunityId: number,
  stageCode: string,
): Promise<StageDashboardView> {
  const res = await client.get<StageDashboardView>(
    `/opportunities/${opportunityId}/stages/${stageCode}/dashboard`,
  );
  return res.data;
}
