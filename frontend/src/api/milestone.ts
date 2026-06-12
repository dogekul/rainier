import client from './client';
import type { PaginatedResult } from '../hooks/usePaginated';

export type MilestoneStatus = 'PLANNED' | 'REACHED' | 'MISSED';

export interface Milestone {
  id: number;
  projectId: number;
  code: string;
  name: string;
  description?: string | null;
  targetDate: string;
  status: MilestoneStatus;
  actualDate?: string | null;
  sortOrder: number;
  createBy?: string;
  createTime?: string;
  updateBy?: string;
  updateTime?: string;
}

export interface MilestoneCreate {
  projectId: number;
  code: string;
  name: string;
  description?: string;
  targetDate: string;
  status?: MilestoneStatus;
  actualDate?: string;
  sortOrder?: number;
}

export interface MilestoneUpdate {
  code: string;
  name: string;
  description?: string;
  targetDate: string;
  status: MilestoneStatus;
  actualDate?: string;
  sortOrder?: number;
}

export interface MilestoneListParams {
  projectId?: number;
  status?: MilestoneStatus;
  page?: number;
  size?: number;
}

export async function listMilestones(
  params: MilestoneListParams = {},
): Promise<PaginatedResult<Milestone>> {
  const res = await client.get<PaginatedResult<Milestone>>('/milestones', { params });
  return res.data;
}

export async function getMilestone(id: number): Promise<Milestone> {
  const res = await client.get<Milestone>(`/milestones/${id}`);
  return res.data;
}

export async function createMilestone(body: MilestoneCreate): Promise<Milestone> {
  const res = await client.post<Milestone>('/milestones', body);
  return res.data;
}

export async function updateMilestone(id: number, body: MilestoneUpdate): Promise<Milestone> {
  const res = await client.put<Milestone>(`/milestones/${id}`, body);
  return res.data;
}

export async function deleteMilestone(id: number): Promise<void> {
  await client.delete(`/milestones/${id}`);
}
