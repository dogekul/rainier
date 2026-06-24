import client from './client';
import type { PaginatedResult } from '../hooks/usePaginated';

export type OperationStage = 'MAINTENANCE' | 'OPERATING' | 'REPURCHASE';
export type OperationStatus = 'ACTIVE' | 'CLOSED';

export const OPR_STAGE_ORDER: OperationStage[] = ['MAINTENANCE', 'OPERATING', 'REPURCHASE'];
export const OPR_STAGE_LABELS: Record<OperationStage, string> = {
  MAINTENANCE: '回款/维保',
  OPERATING: '运营',
  REPURCHASE: '复购',
};

export interface Operation {
  id: number;
  customerName: string;
  title: string;
  stage: OperationStage;
  status: OperationStatus;
  opsOwnerUserId?: number | null;
  opsOwnerName?: string | null;
  projectId?: number | null;
  /** v0.0.58 — 来源商机（可空）。 */
  opportunityId?: number | null;
  createTime?: string;
  updateTime?: string;
}

export interface OperationCreate {
  customerName: string;
  title: string;
  opsOwnerUserId?: number;
  projectId?: number;
  /** v0.0.58 — 可选来源商机 id。 */
  opportunityId?: number;
}

export interface OperationUpdate {
  customerName: string;
  title: string;
  opsOwnerUserId?: number | null;
  projectId?: number | null;
}

export interface OperationListParams {
  stage?: OperationStage;
  status?: OperationStatus;
  /** v0.0.58 — 按来源商机过滤。 */
  opportunityId?: number;
  page?: number;
  size?: number;
}

export async function listOperations(
  params: OperationListParams = {},
): Promise<PaginatedResult<Operation>> {
  const res = await client.get<PaginatedResult<Operation>>('/operations', { params });
  return res.data;
}

export async function createOperation(body: OperationCreate): Promise<Operation> {
  const res = await client.post<Operation>('/operations', body);
  return res.data;
}

export async function advanceOperation(id: number): Promise<Operation> {
  const res = await client.post<Operation>(`/operations/${id}/advance`, {});
  return res.data;
}

/** v0.0.58 — get one operation by id (powers OperationDetailPage). */
export async function getOperation(id: number): Promise<Operation> {
  const res = await client.get<Operation>(`/operations/${id}`);
  return res.data;
}

export async function updateOperation(id: number, body: OperationUpdate): Promise<Operation> {
  const res = await client.put<Operation>(`/operations/${id}`, body);
  return res.data;
}
