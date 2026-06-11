import client from './client';
import type { PaginatedResult } from '../hooks/usePaginated';

export type AuditAction = 'CREATE' | 'UPDATE' | 'DELETE';

export interface AuditLog {
  id: number;
  actor?: string | null;
  entityType: string;
  entityId?: number | null;
  action: AuditAction;
  summary?: string | null;
  createTime?: string;
}

export interface AuditLogListParams {
  actor?: string;
  entityType?: string;
  entityId?: number;
  action?: string;
  page?: number;
  size?: number;
}

export async function listAuditLogs(
  params: AuditLogListParams = {},
): Promise<PaginatedResult<AuditLog>> {
  const res = await client.get<PaginatedResult<AuditLog>>('/audit-logs', { params });
  return res.data;
}

export async function getAuditLog(id: number): Promise<AuditLog> {
  const res = await client.get<AuditLog>(`/audit-logs/${id}`);
  return res.data;
}
