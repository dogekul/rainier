import client from './client';

export interface LoginResponse {
  token: string;
  user: { username: string };
}

/** One role assignment of the current user (v0.0.18; v0.0.20 +adminAccess). */
export interface MeRole {
  roleId: number | null;
  roleCode: string | null;
  roleName: string | null;
  projectId: number | null;
  projectName: string | null;
  projectCode: string | null;
  /** v0.0.20: whether this role grants the full admin console. Never null from the API. */
  adminAccess: boolean;
}

/** A project the current user participates in (v0.0.18). */
export interface MeProject {
  id: number;
  code: string;
  name: string;
}

/** v0.0.18: full current-user context. `id` is null when the token subject has no User. */
export interface MeResponse {
  id: number | null;
  username: string;
  name: string | null;
  roles: MeRole[];
  projects: MeProject[];
}

/** Calls {@code POST /api/auth/login}. */
export async function login(username: string, password: string): Promise<LoginResponse> {
  const { data } = await client.post<LoginResponse>('/auth/login', { username, password });
  return data;
}

/** Calls {@code GET /api/auth/me}; throws on missing/expired token via the response interceptor. */
export async function me(): Promise<MeResponse> {
  const { data } = await client.get<MeResponse>('/auth/me');
  return data;
}
