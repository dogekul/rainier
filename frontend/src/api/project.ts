import client from './client';
import type { PaginatedResult } from '../hooks/usePaginated';

export type ProjectStatus =
  | 'PLANNING'
  | 'ACTIVE'
  | 'ON_HOLD'
  | 'DELIVERED'
  | 'ARCHIVED';

export interface Project {
  id: number;
  code: string;
  name: string;
  description?: string | null;
  status: ProjectStatus;
  ownerUserId: number;
  /** v0.0.8 enrichment — backend join with User. */
  ownerName?: string | null;
  ownerLoginName?: string | null;
  startDate?: string | null;
  endDate?: string | null;
  enabled: boolean;
  createBy?: string;
  createTime?: string;
  updateBy?: string;
  updateTime?: string;
}

export interface ProjectCreate {
  code: string;
  name: string;
  description?: string;
  status?: ProjectStatus;
  ownerUserId: number;
  startDate?: string;
  endDate?: string;
  enabled?: boolean;
}

export interface ProjectUpdate {
  name: string;
  description?: string;
  status: ProjectStatus;
  /** v0.0.8: owner IS mutable (admin can transfer ownership). */
  ownerUserId: number;
  startDate?: string;
  endDate?: string;
  enabled?: boolean;
}

export interface ProjectListParams {
  status?: ProjectStatus;
  enabled?: boolean;
  search?: string;
  page?: number;
  size?: number;
}

export async function listProjects(
  params: ProjectListParams = {},
): Promise<PaginatedResult<Project>> {
  const res = await client.get<PaginatedResult<Project>>('/projects', { params });
  return res.data;
}

export async function getProject(id: number): Promise<Project> {
  const res = await client.get<Project>(`/projects/${id}`);
  return res.data;
}

export async function createProject(body: ProjectCreate): Promise<Project> {
  const res = await client.post<Project>('/projects', body);
  return res.data;
}

export async function updateProject(id: number, body: ProjectUpdate): Promise<Project> {
  const res = await client.put<Project>(`/projects/${id}`, body);
  return res.data;
}

export async function deleteProject(id: number): Promise<void> {
  await client.delete(`/projects/${id}`);
}
