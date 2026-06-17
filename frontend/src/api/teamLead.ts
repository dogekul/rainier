import client from './client';

/** A team (organization) the current user HEADs (v0.0.24). */
export interface LedTeam {
  organizationId: number;
  organizationName: string;
  organizationType: string | null;
}

/** An active member of a team the current user HEADs (v0.0.24). */
export interface TeamMember {
  userId: number;
  name: string | null;
  loginName: string | null;
}

/** GET /api/me/led-teams — teams the caller is an active HEAD of (token-scoped). */
export async function listLedTeams(): Promise<LedTeam[]> {
  const res = await client.get<LedTeam[]>('/me/led-teams');
  return res.data;
}

/** GET /api/me/team-members?organizationId= — active members; 403 if the caller doesn't HEAD it. */
export async function listTeamMembers(organizationId: number): Promise<TeamMember[]> {
  const res = await client.get<TeamMember[]>('/me/team-members', { params: { organizationId } });
  return res.data;
}
