import client from './client';
import type { PaginatedResult } from '../hooks/usePaginated';

export type OrganizationType = 'COMPANY' | 'DEPARTMENT' | 'DOMAIN' | 'TEAM' | 'SUBGROUP';

export interface Organization {
  id: number;
  parentId: number | null;
  type: OrganizationType;
  code: string;
  name: string;
  description?: string | null;
  path?: string | null;
  wholeName?: string | null;
  enabled: boolean;
  createTime?: string;
  updateTime?: string;
}

export interface OrganizationCreate {
  parentId?: number | null;
  type: OrganizationType;
  code: string;
  name: string;
  description?: string;
  enabled?: boolean;
}

export interface OrganizationUpdate {
  code: string;
  name: string;
  description?: string;
  enabled?: boolean;
}

export interface OrganizationListParams {
  type?: OrganizationType;
  parentId?: number;
  search?: string;
  page?: number;
  size?: number;
}

export async function listOrganizations(
  params: OrganizationListParams = {},
): Promise<PaginatedResult<Organization>> {
  const res = await client.get<PaginatedResult<Organization>>('/organizations', { params });
  return res.data;
}

export async function getOrganization(id: number): Promise<Organization> {
  const res = await client.get<Organization>(`/organizations/${id}`);
  return res.data;
}

export async function getOrganizationTree(): Promise<Organization[]> {
  const res = await client.get<Organization[]>('/organizations/tree');
  return res.data;
}

export async function createOrganization(body: OrganizationCreate): Promise<Organization> {
  const res = await client.post<Organization>('/organizations', body);
  return res.data;
}

export async function updateOrganization(
  id: number,
  body: OrganizationUpdate,
): Promise<Organization> {
  const res = await client.put<Organization>(`/organizations/${id}`, body);
  return res.data;
}

export async function moveOrganization(id: number, parentId: number | null): Promise<Organization> {
  const res = await client.put<Organization>(`/organizations/${id}/parent`, { parentId });
  return res.data;
}

export async function deleteOrganization(id: number): Promise<void> {
  await client.delete(`/organizations/${id}`);
}
