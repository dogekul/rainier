import client from './client';
import type { PaginatedResult } from '../hooks/usePaginated';

export type PositionCategory = 'TECH' | 'BIZ' | 'PM' | 'MGMT' | 'OTHER';

export interface Position {
  id: number;
  code: string;
  name: string;
  description?: string | null;
  category: PositionCategory;
  enabled: boolean;
  createBy?: string;
  createTime?: string;
  updateBy?: string;
  updateTime?: string;
}

export interface PositionCreate {
  code: string;
  name: string;
  description?: string;
  category: PositionCategory;
  enabled?: boolean;
}

export interface PositionUpdate {
  name: string;
  description?: string;
  category: PositionCategory;
  enabled?: boolean;
}

export interface PositionListParams {
  category?: PositionCategory;
  enabled?: boolean;
  search?: string;
  page?: number;
  size?: number;
}

export async function listPositions(
  params: PositionListParams = {},
): Promise<PaginatedResult<Position>> {
  const res = await client.get<PaginatedResult<Position>>('/positions', { params });
  return res.data;
}

export async function getPosition(id: number): Promise<Position> {
  const res = await client.get<Position>(`/positions/${id}`);
  return res.data;
}

export async function createPosition(body: PositionCreate): Promise<Position> {
  const res = await client.post<Position>('/positions', body);
  return res.data;
}

export async function updatePosition(id: number, body: PositionUpdate): Promise<Position> {
  const res = await client.put<Position>(`/positions/${id}`, body);
  return res.data;
}

export async function deletePosition(id: number): Promise<void> {
  await client.delete(`/positions/${id}`);
}
