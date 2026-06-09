import client from './client';
import type { PaginatedResult } from '../hooks/usePaginated';

export type FeatureStatus = 'PLANNING' | 'ACTIVE' | 'DEPRECATED';

export interface Feature {
  id: number;
  code: string;
  name: string;
  description?: string | null;
  status: FeatureStatus;
  moduleId: number;
  moduleCode?: string | null;
  moduleName?: string | null;
  ownerUserId: number;
  ownerName?: string | null;
  ownerLoginName?: string | null;
  createBy?: string;
  createTime?: string;
  updateBy?: string;
  updateTime?: string;
}

export interface FeatureCreate {
  code: string;
  name: string;
  description?: string;
  status?: FeatureStatus;
  moduleId: number;
  ownerUserId: number;
}

export interface FeatureUpdate {
  code: string;
  name: string;
  description?: string;
  status: FeatureStatus;
  ownerUserId: number;
}

export interface FeatureListParams {
  moduleId?: number;
  status?: FeatureStatus;
  search?: string;
  page?: number;
  size?: number;
}

export async function listFeatures(
  params: FeatureListParams = {},
): Promise<PaginatedResult<Feature>> {
  const res = await client.get<PaginatedResult<Feature>>('/features', { params });
  return res.data;
}

export async function getFeature(id: number): Promise<Feature> {
  const res = await client.get<Feature>(`/features/${id}`);
  return res.data;
}

export async function createFeature(body: FeatureCreate): Promise<Feature> {
  const res = await client.post<Feature>('/features', body);
  return res.data;
}

export async function updateFeature(id: number, body: FeatureUpdate): Promise<Feature> {
  const res = await client.put<Feature>(`/features/${id}`, body);
  return res.data;
}

export async function deleteFeature(id: number): Promise<void> {
  await client.delete(`/features/${id}`);
}
