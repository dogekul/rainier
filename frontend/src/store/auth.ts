import { create } from 'zustand';

/** User identity carried by the auth store. */
export interface AuthUser {
  username: string;
}

interface AuthState {
  token: string | null;
  user: AuthUser | null;
  setAuth: (token: string, user: AuthUser) => void;
  logout: () => void;
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
