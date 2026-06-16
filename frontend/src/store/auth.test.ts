// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { TOKEN_STORAGE_KEY, isElevated, useAuthStore } from './auth';
import type { MeRole } from '../api/auth';

function role(adminAccess: boolean): MeRole {
  return {
    roleId: 1,
    roleCode: 'R',
    roleName: 'R',
    projectId: null,
    projectName: null,
    projectCode: null,
    adminAccess,
  };
}

/**
 * Foundation for SLICE-F03. No direct TC but underpins TC-FES-001 / TC-FES-002 by guaranteeing
 * the store ↔ localStorage contract used by ProtectedRoute and the login flow.
 */
describe('useAuthStore', () => {
  beforeEach(() => {
    window.localStorage.removeItem(TOKEN_STORAGE_KEY);
    useAuthStore.setState({ token: null, user: null });
  });

  afterEach(() => {
    window.localStorage.removeItem(TOKEN_STORAGE_KEY);
  });

  it('setAuth writes token to state and to localStorage', () => {
    useAuthStore.getState().setAuth('jwt-token-xyz', { username: 'alice' });

    const state = useAuthStore.getState();
    expect(state.token).toBe('jwt-token-xyz');
    expect(state.user).toEqual({ username: 'alice' });
    expect(window.localStorage.getItem(TOKEN_STORAGE_KEY)).toBe('jwt-token-xyz');
  });

  it('logout clears state and removes token from localStorage', () => {
    useAuthStore.getState().setAuth('jwt-token-xyz', { username: 'alice' });
    useAuthStore.getState().logout();

    const state = useAuthStore.getState();
    expect(state.token).toBeNull();
    expect(state.user).toBeNull();
    expect(window.localStorage.getItem(TOKEN_STORAGE_KEY)).toBeNull();
  });
});

/** v0.0.20 TC-FES-RN-007: isElevated reflects ANY admin role. */
describe('isElevated', () => {
  it('is true when any role grants adminAccess', () => {
    expect(isElevated({ username: 'a', roles: [role(false), role(true)] })).toBe(true);
  });

  it('is false when no role grants adminAccess', () => {
    expect(isElevated({ username: 'a', roles: [role(false), role(false)] })).toBe(false);
  });

  it('is false for a user with no roles or a null user', () => {
    expect(isElevated({ username: 'a' })).toBe(false);
    expect(isElevated(null)).toBe(false);
    expect(isElevated(undefined)).toBe(false);
  });
});
