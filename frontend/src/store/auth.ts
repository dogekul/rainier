import { create } from 'zustand';
import type { MeProject, MeRole } from '../api/auth';

/**
 * User identity carried by the auth store.
 *
 * <p>v0.0.18: extended with the current-user context (id/name/roles/projects) from GET /api/auth/me.
 * All but `username` are optional — login sets only `{username}`, then the workbench upgrades it.
 */
export interface AuthUser {
  username: string;
  id?: number | null;
  name?: string | null;
  roles?: MeRole[];
  projects?: MeProject[];
}

interface AuthState {
  token: string | null;
  user: AuthUser | null;
  setAuth: (token: string, user: AuthUser) => void;
  logout: () => void;
}

/**
 * v0.0.20: a user is "elevated" (admin) when ANY of their roles grants admin access. Drives the
 * role-scoped navigation (which Sider groups show) and the admin route guards. Org-level and
 * project-level roles are treated alike — elevation is global, not per-project.
 */
export function isElevated(user: AuthUser | null | undefined): boolean {
  return (user?.roles ?? []).some((r) => r.adminAccess === true);
}

/** localStorage key used to persist the bearer token across reloads. */
export const TOKEN_STORAGE_KEY = 'rainier.token';

function readInitialToken(): string | null {
  if (typeof window === 'undefined') return null;
  try {
    return window.localStorage.getItem(TOKEN_STORAGE_KEY);
  } catch {
    return null;
  }
}

/**
 * Zustand store for the auth placeholder flow.
 *
 * <p>The {@code user} object is intentionally NOT persisted: on a cold reload we have the token
 * but need to call {@code GET /api/auth/me} to (re)hydrate the user. SLICE-F04 wires this.
 */
export const useAuthStore = create<AuthState>((set) => ({
  token: readInitialToken(),
  user: null,
  setAuth: (token, user) => {
    try {
      window.localStorage.setItem(TOKEN_STORAGE_KEY, token);
    } catch {
      // localStorage unavailable (private mode, SSR) — in-memory state still works.
    }
    set({ token, user });
  },
  logout: () => {
    try {
      window.localStorage.removeItem(TOKEN_STORAGE_KEY);
    } catch {
      // ignore
    }
    set({ token: null, user: null });
  },
}));
