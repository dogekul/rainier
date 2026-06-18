import client from './client';

/** v0.0.40 — one active org membership of the current user. */
export interface ProfileMembership {
  organizationId: number;
  organizationName: string;
  organizationType: string | null;
  role: string;
  isPrimary: boolean;
}

/** The current user's direct manager (first non-self HEAD up the org tree). */
export interface ProfileManager {
  userId: number;
  name: string | null;
  loginName: string | null;
}

/** GET /api/me/profile payload — org identity + position + manager + contribution counts. */
export interface UserProfile {
  userId: number | null;
  loginName: string;
  name: string | null;
  positionName: string | null;
  positionCategory: string | null;
  memberships: ProfileMembership[];
  manager: ProfileManager | null;
  ownedStoryCount: number;
  assignedTaskCount: number;
}

/** GET /api/me/profile — the current user's contribution / capability profile. */
export async function getMyProfile(): Promise<UserProfile> {
  const res = await client.get<UserProfile>('/me/profile');
  return res.data;
}
