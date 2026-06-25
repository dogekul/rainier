import client from './client';
import type { AuditLog } from './auditLog';

/** v0.0.41 — one bucket in an audit breakdown (action or entityType + its count). */
export interface LabelCount {
  label: string;
  count: number;
}

/** Audit-activity rollup for the compliance dashboard. */
export interface AuditSummary {
  total: number;
  byAction: LabelCount[];
  byEntityType: LabelCount[];
  recent: AuditLog[];
}

/** A disabled user who still holds role grants (should be revoked). */
export interface ResidualPermission {
  userId: number;
  name: string | null;
  loginName: string | null;
  roleCount: number;
  roleNames: string[];
}

/** GET /api/compliance/audit-summary — admin-only audit-activity aggregation. */
export async function getAuditSummary(): Promise<AuditSummary> {
  const res = await client.get<AuditSummary>('/compliance/audit-summary');
  return res.data;
}

/** GET /api/compliance/residual-permissions — admin-only 停用-残留权限 reconciliation. */
export async function getResidualPermissions(): Promise<ResidualPermission[]> {
  const res = await client.get<ResidualPermission[]>('/compliance/residual-permissions');
  return res.data;
}

/** v0.0.80 B7 — result of a residual-permission recycle call (revoke or disable+revoke). */
export interface RevokeResult {
  ok: boolean;
  revokedCount: number;
  alreadyDisabled: boolean;
}

/** POST /api/compliance/users/{id}/revoke-roles — hard-delete all UserRoles of a disabled user. */
export async function revokeResidualRoles(userId: number): Promise<RevokeResult> {
  const res = await client.post<RevokeResult>(`/compliance/users/${userId}/revoke-roles`);
  return res.data;
}

/** POST /api/compliance/disable-user/{id} — disable + revoke all UserRoles in one step. */
export async function disableUserAndRevoke(userId: number): Promise<RevokeResult> {
  const res = await client.post<RevokeResult>(`/compliance/disable-user/${userId}`);
  return res.data;
}
