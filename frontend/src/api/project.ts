import client from './client';
import type { PaginatedResult } from '../hooks/usePaginated';

export type ProjectStatus =
  | 'PLANNING'
  | 'ACTIVE'
  | 'ON_HOLD'
  | 'DELIVERED'
  | 'ARCHIVED';

/** v0.0.16 — extensible project type; seeded with 轻量(CASUAL)/正式(FORMAL). */
export type ProjectType = 'CASUAL' | 'FORMAL';

export interface Project {
  id: number;
  code: string;
  name: string;
  description?: string | null;
  status: ProjectStatus;
  /** v0.0.16 — backend read-coalesces a legacy NULL to CASUAL, so this is always set. */
  projectType: ProjectType;
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
  /** v0.0.16 — optional; omitted → backend defaults to CASUAL. */
  projectType?: ProjectType;
  ownerUserId: number;
  startDate?: string;
  endDate?: string;
  enabled?: boolean;
}

export interface ProjectUpdate {
  name: string;
  description?: string;
  status: ProjectStatus;
  /** v0.0.16 — set to convert 轻量↔正式; omitted → backend preserves current value. */
  projectType?: ProjectType;
  /** v0.0.8: owner IS mutable (admin can transfer ownership). */
  ownerUserId: number;
  startDate?: string;
  endDate?: string;
  enabled?: boolean;
}

export interface ProjectListParams {
  status?: ProjectStatus;
  projectType?: ProjectType;
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
