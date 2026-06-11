import client from './client';
import type { PaginatedResult } from '../hooks/usePaginated';

/** A SprintFeatureLink row. */
export interface SprintFeatureLink {
  id: number;
  sprintId: number;
  featureId: number;
  createTime?: string;
  createBy?: string;
}

/** Feature enriched for GET /api/sprints/{id}/features. */
export interface SprintFeatureView {
  linkId: number;
  featureId: number;
  code: string;
  name: string;
  status: string;
  moduleId: number;
}

/** Sprint enriched for GET /api/features/{id}/sprints. */
export interface FeatureSprintView {
  linkId: number;
  sprintId: number;
  code: string;
  name: string;
  status: string;
  requirementId: number;
  productId?: number | null;
}

export interface SprintFeatureCreate {
  sprintId: number;
  featureId: number;
}

export async function createSprintFeature(
  body: SprintFeatureCreate,
): Promise<SprintFeatureLink> {
  const res = await client.post<SprintFeatureLink>('/sprint-features', body);
  return res.data;
}

export async function deleteSprintFeature(id: number): Promise<void> {
  await client.delete(`/sprint-features/${id}`);
}

export interface SprintFeatureListParams {
  sprintId?: number;
  featureId?: number;
  page?: number;
  size?: number;
}

export async function listSprintFeatures(
  params: SprintFeatureListParams = {},
): Promise<PaginatedResult<SprintFeatureLink>> {
  const res = await client.get<PaginatedResult<SprintFeatureLink>>('/sprint-features', {
    params,
  });
  return res.data;
}

/** Reverse: features linked to a sprint. */
export async function getSprintFeatures(sprintId: number): Promise<SprintFeatureView[]> {
  const res = await client.get<SprintFeatureView[]>(`/sprints/${sprintId}/features`);
  return res.data;
}

/** Reverse: sprints (iterations) a feature is in. */
export async function getFeatureSprints(featureId: number): Promise<FeatureSprintView[]> {
  const res = await client.get<FeatureSprintView[]>(`/features/${featureId}/sprints`);
  return res.data;
}

/** Reverse: features reached by a requirement through its sprints (2-hop, deduped). */
export async function getRequirementFeatures(
  requirementId: number,
): Promise<SprintFeatureView[]> {
  const res = await client.get<SprintFeatureView[]>(
    `/requirements/${requirementId}/features`,
  );
  return res.data;
}
