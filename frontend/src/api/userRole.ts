import client from './client';
import type { PaginatedResult } from '../hooks/usePaginated';

export interface UserRoleLink {
  id: number;
  userId: number;
  roleId: number;
  projectId: number | null;
  /** v0.0.7 enrichment — populated by backend service via join. */
  userName?: string;
  userLoginName?: string;
  roleName?: string;
  roleCode?: string;
  /** v0.0.8 enrichment — populated when projectId is non-null. */
  projectName?: string | null;
  projectCode?: string | null;
  createBy?: string;
  createTime?: string;
}

export interface UserRoleLinkCreate {
  userId: number;
  roleId: number;
  /** Placeholder FK to future Project entity. Pass null for company-wide / unassigned hat. */
  projectId?: number | null;
}

export interface UserRoleListParams {
  userId?: number;
  roleId?: number;
  projectId?: number;
  page?: number;
  size?: number;
}

export async function listUserRoles(
  params: UserRoleListParams = {},
): Promise<PaginatedResult<UserRoleLink>> {
  const res = await client.get<PaginatedResult<UserRoleLink>>('/user-roles', { params });
  return res.data;
}

export async function getUserRole(id: number): Promise<UserRoleLink> {
  const res = await client.get<UserRoleLink>(`/user-roles/${id}`);
  return res.data;
}

export async function createUserRole(body: UserRoleLinkCreate): Promise<UserRoleLink> {
  const res = await client.post<UserRoleLink>('/user-roles', body);
  return res.data;
}

export async function deleteUserRole(id: number): Promise<void> {
  await client.delete(`/user-roles/${id}`);
}
