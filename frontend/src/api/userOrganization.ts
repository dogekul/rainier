import client from './client';
import type { PaginatedResult } from '../hooks/usePaginated';
import type { OrganizationType } from './organization';

export type UserOrgRole = 'MEMBER' | 'HEAD';

export interface UserOrganization {
  id: number;
  userId: number;
  userLoginName?: string | null;
  userName?: string | null;
  organizationId: number;
  organizationName?: string | null;
  organizationType?: OrganizationType | null;
  role: UserOrgRole;
  isPrimary: boolean;
  joinedAt?: string;
  leftAt?: string | null;
  createTime?: string;
  updateTime?: string;
}

export interface UserOrgCreate {
  userId: number;
  organizationId: number;
  role?: UserOrgRole;
  isPrimary?: boolean;
  joinedAt?: string;
}

export interface UserOrgUpdate {
  organizationId?: number;
  role?: UserOrgRole;
  isPrimary?: boolean;
  leftAt?: string | null;
}

export interface UserOrgListParams {
  userId?: number;
  organizationId?: number;
  role?: UserOrgRole;
  activeOnly?: boolean;
  page?: number;
  size?: number;
}

export async function listUserOrganizations(
  params: UserOrgListParams = {},
): Promise<PaginatedResult<UserOrganization>> {
  const res = await client.get<PaginatedResult<UserOrganization>>('/user-organizations', {
    params,
  });
  return res.data;
}

export async function getUserOrganization(id: number): Promise<UserOrganization> {
  const res = await client.get<UserOrganization>(`/user-organizations/${id}`);
  return res.data;
}

export async function createUserOrganization(body: UserOrgCreate): Promise<UserOrganization> {
  const res = await client.post<UserOrganization>('/user-organizations', body);
  return res.data;
}

export async function updateUserOrganization(
  id: number,
  body: UserOrgUpdate,
): Promise<UserOrganization> {
  const res = await client.put<UserOrganization>(`/user-organizations/${id}`, body);
  return res.data;
}

export async function deleteUserOrganization(id: number): Promise<void> {
  await client.delete(`/user-organizations/${id}`);
}
