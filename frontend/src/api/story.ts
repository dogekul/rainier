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
  requirementId: number;
  /** v0.0.9 enrichment — backend joins Requirement. */
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
  /** Required; backend copies parent Requirement's projectId at creation. */
  requirementId: number;
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
  closeReason?: string;
}

export interface StoryListParams {
  requirementId?: number;
  status?: StoryStatus;
  priority?: Priority;
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
