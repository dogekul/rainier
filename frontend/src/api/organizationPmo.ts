import client from './client';

/** v0.0.64 — Organization ↔ PMO 关系 API. */

export interface OrganizationPmoDetail {
  id: number;
  organizationId: number;
  organizationName?: string | null;
  userId: number;
  userName?: string | null;
  userLoginName?: string | null;
  createTime?: string;
}

export interface EffectivePmoDetail {
  userId: number;
  userName?: string | null;
  userLoginName?: string | null;
  inheritedFromOrgId: number;
  inheritedFromOrgName?: string | null;
}

export async function listOrganizationPmos(organizationId: number): Promise<OrganizationPmoDetail[]> {
  const res = await client.get<OrganizationPmoDetail[]>(
    `/organizations/${organizationId}/pmos`,
  );
  return res.data;
}

export async function listEffectivePmos(organizationId: number): Promise<EffectivePmoDetail[]> {
  const res = await client.get<EffectivePmoDetail[]>(
    `/organizations/${organizationId}/effective-pmos`,
  );
  return res.data;
}

export async function addOrganizationPmo(
  organizationId: number,
  userId: number,
): Promise<OrganizationPmoDetail> {
  const res = await client.post<OrganizationPmoDetail>(
    `/organizations/${organizationId}/pmos`,
    { userId },
  );
  return res.data;
}

export async function removeOrganizationPmo(
  organizationId: number,
  userId: number,
): Promise<void> {
  await client.delete(`/organizations/${organizationId}/pmos/${userId}`);
}
