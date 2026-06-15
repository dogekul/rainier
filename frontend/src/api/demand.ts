import client from './client';
import type { PaginatedResult } from '../hooks/usePaginated';

export type DemandStatus = 'PENDING' | 'IN_REVIEW' | 'CONVERTED' | 'DONE' | 'CLOSED';
export type Priority = 'URGENT' | 'HIGH' | 'MEDIUM' | 'LOW' | 'LOWEST';

/** v0.0.19 — shared 中文 labels for the 5-level Priority (used by every priority dropdown). */
export const PRIORITY_LABELS: Record<Priority, string> = {
  URGENT: '紧急',
  HIGH: '高',
  MEDIUM: '中',
  LOW: '低',
  LOWEST: '最低',
};
export type Source = 'WEB' | 'WECHAT' | 'EMAIL' | 'DINGTALK' | 'OTHER';

export interface Demand {
  id: number;
  title: string;
  description?: string | null;
  submitterUserId: number;
  status: DemandStatus;
  priority: Priority;
  source: Source;
  aiClassification?: string | null;
  aiDuplicateHint?: number | null;
  closeReason?: string | null;
  createBy?: string;
  createTime?: string;
  updateBy?: string;
  updateTime?: string;
}

export interface DemandCreate {
  title: string;
  description?: string;
  submitterUserId: number;
  status?: DemandStatus;
  priority?: Priority;
  source?: Source;
  closeReason?: string;
}

export interface DemandUpdate {
  title: string;
  description?: string;
  status?: DemandStatus;
  priority?: Priority;
  source?: Source;
  closeReason?: string;
}

export interface DemandListParams {
  status?: DemandStatus;
  priority?: Priority;
  search?: string;
  page?: number;
  size?: number;
}

export interface DerivedRequirementView {
  requirementId: number;
  code: string;
  title: string;
  description?: string | null;
  ownerUserId: number;
  status: string;
  priority: string;
  complexity?: string | null;
  linkId: number;
  linkType: string;
}

export async function listDemands(
  params: DemandListParams = {},
): Promise<PaginatedResult<Demand>> {
  const res = await client.get<PaginatedResult<Demand>>('/demands', { params });
  return res.data;
}

export async function getDemand(id: number): Promise<Demand> {
  const res = await client.get<Demand>(`/demands/${id}`);
  return res.data;
}

export async function createDemand(body: DemandCreate): Promise<Demand> {
  const res = await client.post<Demand>('/demands', body);
  return res.data;
}

export async function updateDemand(id: number, body: DemandUpdate): Promise<Demand> {
  const res = await client.put<Demand>(`/demands/${id}`, body);
  return res.data;
}

export async function deleteDemand(id: number): Promise<void> {
  await client.delete(`/demands/${id}`);
}

export async function getDerivedRequirements(
  id: number,
): Promise<DerivedRequirementView[]> {
  const res = await client.get<DerivedRequirementView[]>(`/demands/${id}/derived-requirements`);
  return res.data;
}
