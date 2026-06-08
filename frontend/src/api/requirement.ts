import client from './client';
import type { PaginatedResult } from '../hooks/usePaginated';
import type { Priority } from './demand';

export type RequirementStatus =
  | 'DRAFT'
  | 'IN_REVIEW'
  | 'APPROVED'
  | 'IN_DEV'
  | 'DELIVERED'
  | 'DEPRECATED';

export type Complexity = 'XS' | 'S' | 'M' | 'L' | 'XL';

export interface Requirement {
  id: number;
  code: string;
  title: string;
  description?: string | null;
  ownerUserId: number;
  ownerName?: string | null;
  ownerLoginName?: string | null;
  status: RequirementStatus;
  priority: Priority;
  complexity?: Complexity | null;
  projectId?: number | null;
  /** v0.0.8: enrichment from join with Project. */
  projectName?: string | null;
  projectCode?: string | null;
  /** v0.0.10 enrichment — count of non-deleted Sprints under this Requirement (replaces v0.0.9 storyCount). */
  sprintCount?: number;
  closeReason?: string | null;
  createBy?: string;
  createTime?: string;
  updateBy?: string;
  updateTime?: string;
}

export interface RequirementCreate {
  code: string;
  title: string;
  description?: string;
  ownerUserId: number;
  status?: RequirementStatus;
  priority?: Priority;
  complexity?: Complexity;
  projectId?: number;
  closeReason?: string;
  /** Optional: atomically create N derived links to existing demands. */
  sourceDemandIds?: number[];
}

export interface RequirementUpdate {
  code: string;
  title: string;
  description?: string;
  /** v0.0.8: owner IS mutable (semantic reversal of v0.0.6). Required by backend @NotNull. */
  ownerUserId: number;
  status?: RequirementStatus;
  priority?: Priority;
  complexity?: Complexity;
  /** v0.0.8: null explicitly clears projectId (backend accepts unset for null). */
  projectId?: number | null;
  closeReason?: string;
}

export interface RequirementListParams {
  status?: RequirementStatus;
  priority?: Priority;
  projectId?: number;
  search?: string;
  page?: number;
  size?: number;
}

export interface SourceDemandView {
  demandId: number;
  title: string;
  description?: string | null;
  submitterUserId: number;
  status: string;
  priority: string;
  source: string;
  linkId: number;
  linkType: string;
}

export async function listRequirements(
  params: RequirementListParams = {},
): Promise<PaginatedResult<Requirement>> {
  const res = await client.get<PaginatedResult<Requirement>>('/requirements', { params });
  return res.data;
}

export async function getRequirement(id: number): Promise<Requirement> {
  const res = await client.get<Requirement>(`/requirements/${id}`);
  return res.data;
}

export async function createRequirement(body: RequirementCreate): Promise<Requirement> {
  const res = await client.post<Requirement>('/requirements', body);
  return res.data;
}

export async function updateRequirement(
  id: number,
  body: RequirementUpdate,
): Promise<Requirement> {
  const res = await client.put<Requirement>(`/requirements/${id}`, body);
  return res.data;
}

export async function deleteRequirement(id: number): Promise<void> {
  await client.delete(`/requirements/${id}`);
}

export async function getSourceDemands(id: number): Promise<SourceDemandView[]> {
  const res = await client.get<SourceDemandView[]>(`/requirements/${id}/source-demands`);
  return res.data;
}
