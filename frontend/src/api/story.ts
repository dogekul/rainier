import client from './client';
import type { PaginatedResult } from '../hooks/usePaginated';
import type { Priority } from './demand';
import type { Complexity } from './requirement';

export type StoryStatus =
  | 'DRAFT'
  | 'READY'
  | 'IN_PROGRESS'
  | 'DONE'
  | 'BLOCKED'
  | 'CANCELLED';

export interface Story {
  id: number;
  code: string;
  title: string;
  description?: string | null;
  acceptanceCriteria?: string | null;
  status: StoryStatus;
  priority: Priority;
  complexity?: Complexity | null;
  /** v0.0.10: Story now belongs to Sprint, not directly to Requirement. */
  sprintId: number;
  sprintCode?: string | null;
  sprintName?: string | null;
  sprintStatus?: string | null;
  /** Populated via 2-stage enrichment sprint → requirement. */
  requirementId?: number | null;
  requirementCode?: string | null;
  requirementTitle?: string | null;
  projectId?: number | null;
  /** v0.0.9 enrichment — backend joins Project (null when projectId is null). */
  projectName?: string | null;
  projectCode?: string | null;
  ownerUserId: number;
  /** v0.0.9 enrichment — backend joins User. */
  ownerName?: string | null;
  ownerLoginName?: string | null;
  /** v0.0.39 review fields — exposed to the编辑 UI as of v0.0.81. */
  reviewerUserId?: number | null;
  reviewStatus?: 'PENDING' | 'APPROVED' | 'REJECTED' | null;
  reviewerName?: string | null;
  closeReason?: string | null;
  createBy?: string;
  createTime?: string;
  updateBy?: string;
  updateTime?: string;
}

export interface StoryCreate {
  code: string;
  title: string;
  description?: string;
  acceptanceCriteria?: string;
  status?: StoryStatus;
  priority?: Priority;
  complexity?: Complexity;
  /** Required; backend copies grandparent Requirement's projectId transitively at creation. */
  sprintId: number;
  ownerUserId: number;
  closeReason?: string;
}

export interface StoryUpdate {
  code: string;
  title: string;
  description?: string;
  acceptanceCriteria?: string;
  status: StoryStatus;
  priority: Priority;
  complexity?: Complexity;
  /** v0.0.9: owner IS mutable (sibling of v0.0.8 Decision 6b). */
  ownerUserId: number;
  /**
   * v0.0.81: optional reviewer reassignment. Patch-like — key absent → backend keeps
   * existing reviewer; explicit null → clears; number → replaces. The编辑 UI sends this
   * key only when the user actively changed reviewer; default save omits it so the
   * existing review-queue assignment 不会被覆盖。
   */
  reviewerUserId?: number | null;
  closeReason?: string;
}

export interface StoryListParams {
  projectId?: number;
  sprintId?: number;
  status?: StoryStatus;
  priority?: Priority;
  /** v0.0.18: filter to a single owner (powers 我的 Story on the workbench). */
  ownerUserId?: number;
  search?: string;
  page?: number;
  size?: number;
}

export async function listStories(
  params: StoryListParams = {},
): Promise<PaginatedResult<Story>> {
  const res = await client.get<PaginatedResult<Story>>('/stories', { params });
  return res.data;
}

export async function getStory(id: number): Promise<Story> {
  const res = await client.get<Story>(`/stories/${id}`);
  return res.data;
}

export async function createStory(body: StoryCreate): Promise<Story> {
  const res = await client.post<Story>('/stories', body);
  return res.data;
}

export async function updateStory(id: number, body: StoryUpdate): Promise<Story> {
  const res = await client.put<Story>(`/stories/${id}`, body);
  return res.data;
}

export async function deleteStory(id: number): Promise<void> {
  await client.delete(`/stories/${id}`);
}
