import client from './client';

export interface LoginResponse {
  token: string;
  user: { username: string };
}

export interface MeResponse {
  username: string;
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
