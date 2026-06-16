import client from './client';
import type { PaginatedResult } from '../hooks/usePaginated';

export interface Role {
  id: number;
  code: string;
  name: string;
  description?: string | null;
  enabled: boolean;
  /** v0.0.20: grants the full admin console (false for legacy/unset). */
  adminAccess: boolean;
  createBy?: string;
  createTime?: string;
  updateBy?: string;
  updateTime?: string;
}

export interface RoleCreate {
  code: string;
  name: string;
  description?: string;
  enabled?: boolean;
  adminAccess?: boolean;
}

export interface RoleUpdate {
  name: string;
  description?: string;
  enabled?: boolean;
  adminAccess?: boolean;
}

export interface RoleListParams {
  enabled?: boolean;
  search?: string;
  page?: number;
  size?: number;
}

export async function listRoles(params: RoleListParams = {}): Promise<PaginatedResult<Role>> {
  const res = await client.get<PaginatedResult<Role>>('/roles', { params });
  return res.data;
}

export async function getRole(id: number): Promise<Role> {
  const res = await client.get<Role>(`/roles/${id}`);
  return res.data;
}

export async function createRole(body: RoleCreate): Promise<Role> {
  const res = await client.post<Role>('/roles', body);
  return res.data;
}

export async function updateRole(id: number, body: RoleUpdate): Promise<Role> {
  const res = await client.put<Role>(`/roles/${id}`, body);
  return res.data;
}

export async function deleteRole(id: number): Promise<void> {
  await client.delete(`/roles/${id}`);
}
