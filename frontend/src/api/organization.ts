import client from './client';
import type { PaginatedResult } from '../hooks/usePaginated';

export type OrganizationType = 'COMPANY' | 'DEPARTMENT' | 'DOMAIN' | 'TEAM' | 'SUBGROUP';

export interface Organization {
  id: string;
  parentId: string | null;
  type: OrganizationType;
  code: string;
  name: string;
  description?: string | null;
  path?: string | null;
  wholeName?: string | null;
  isPmo: boolean;
  enabled: boolean;
  createTime?: string;
  updateTime?: string;
}

export interface OrganizationCreate {
  parentId?: string | null;
  type: OrganizationType;
  code: string;
  name: string;
  description?: string;
  isPmo?: boolean;
  enabled?: boolean;
}

export interface OrganizationUpdate {
  name: string;
  description?: string;
  isPmo?: boolean;
  enabled?: boolean;
}

export interface OrganizationListParams {
  type?: OrganizationType;
  parentId?: string;
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

export async function getOrganization(id: string): Promise<Organization> {
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

export async function updateOrganization(id: string, body: OrganizationUpdate): Promise<Organization> {
  const res = await client.put<Organization>(`/organizations/${id}`, body);
  return res.data;
}

export async function moveOrganization(id: string, parentId: string | null): Promise<Organization> {
  const res = await client.put<Organization>(`/organizations/${id}/parent`, { parentId });
  return res.data;
}

export async function deleteOrganization(id: string): Promise<void> {
  await client.delete(`/organizations/${id}`);
}
