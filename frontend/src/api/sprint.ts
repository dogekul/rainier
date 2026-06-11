import client from './client';
import type { PaginatedResult } from '../hooks/usePaginated';

export type SprintStatus = 'PLANNING' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';

export interface Sprint {
  id: number;
  code: string;
  name: string;
  description?: string | null;
  goal?: string | null;
  status: SprintStatus;
  requirementId: number;
  requirementCode?: string | null;
  requirementTitle?: string | null;
  projectId?: number | null;
  projectName?: string | null;
  projectCode?: string | null;
  /** v0.0.14: the product this sprint iterates on (null until first feature linked). */
  productId?: number | null;
  productName?: string | null;
  ownerUserId: number;
  ownerName?: string | null;
  ownerLoginName?: string | null;
  startDate?: string | null;
  endDate?: string | null;
  storyCount?: number;
  createBy?: string;
  createTime?: string;
  updateBy?: string;
  updateTime?: string;
}

export interface SprintCreate {
  code: string;
  name: string;
  description?: string;
  goal?: string;
  status?: SprintStatus;
  requirementId: number;
  /** v0.0.14: optional create-time product pre-bind; otherwise established on first feature link. */
  productId?: number;
  ownerUserId: number;
  startDate?: string;
  endDate?: string;
}

export interface SprintUpdate {
  code: string;
  name: string;
  description?: string;
  goal?: string;
  status: SprintStatus;
  ownerUserId: number;
  startDate?: string;
  endDate?: string;
}

export interface SprintListParams {
  projectId?: number;
  requirementId?: number;
  status?: SprintStatus;
  search?: string;
  page?: number;
  size?: number;
}

export async function listSprints(
  params: SprintListParams = {},
): Promise<PaginatedResult<Sprint>> {
  const res = await client.get<PaginatedResult<Sprint>>('/sprints', { params });
  return res.data;
}

export async function getSprint(id: number): Promise<Sprint> {
  const res = await client.get<Sprint>(`/sprints/${id}`);
  return res.data;
}

export async function createSprint(body: SprintCreate): Promise<Sprint> {
  const res = await client.post<Sprint>('/sprints', body);
  return res.data;
}

export async function updateSprint(id: number, body: SprintUpdate): Promise<Sprint> {
  const res = await client.put<Sprint>(`/sprints/${id}`, body);
  return res.data;
}

export async function deleteSprint(id: number): Promise<void> {
  await client.delete(`/sprints/${id}`);
}
