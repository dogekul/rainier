import client from './client';
import type { PaginatedResult } from '../hooks/usePaginated';

export type LinkType = 'DERIVED' | 'RELATED';

export interface DemandRequirementLink {
  id: number;
  demandId: number;
  requirementId: number;
  linkType: LinkType;
  createBy?: string;
  createTime?: string;
  updateBy?: string;
  updateTime?: string;
}

export interface DemandRequirementLinkCreate {
  demandId: number;
  requirementId: number;
  linkType: LinkType;
}

export interface DemandRequirementListParams {
  demandId?: number;
  requirementId?: number;
  page?: number;
  size?: number;
}

export async function listDemandRequirements(
  params: DemandRequirementListParams = {},
): Promise<PaginatedResult<DemandRequirementLink>> {
  const res = await client.get<PaginatedResult<DemandRequirementLink>>(
    '/demand-requirements',
    { params },
  );
  return res.data;
}

export async function getDemandRequirement(id: number): Promise<DemandRequirementLink> {
  const res = await client.get<DemandRequirementLink>(`/demand-requirements/${id}`);
  return res.data;
}

export async function createDemandRequirement(
  body: DemandRequirementLinkCreate,
): Promise<DemandRequirementLink> {
  const res = await client.post<DemandRequirementLink>('/demand-requirements', body);
  return res.data;
}

export async function deleteDemandRequirement(id: number): Promise<void> {
  await client.delete(`/demand-requirements/${id}`);
}
